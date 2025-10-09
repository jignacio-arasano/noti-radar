package scrapping.noti_radar.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.ClickTrackingSetting;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.TrackingSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import scrapping.noti_radar.config.MonitorProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MonitorProperties props;
    private final String sendGridApiKey;

    public NotificationService(MonitorProperties props,
                               @Value("${SENDGRID_API_KEY:}") String sendGridApiKey) {
        this.props = props;
        this.sendGridApiKey = sendGridApiKey;
    }

    /**
     * Envía un mail con el cuerpo exactamente en el formato solicitado, filtrando
     * encabezados duplicados que puedan venir en el summary (p. ej. "++ Añadido:" / "-- Eliminado:").
     *
     * Formato del cuerpo:
     *
     * Página monitoreada: <monitoredUrl>
     *
     * Resumen de cambios:
     * ++ Añadido:
     * + <líneas agregadas>
     * link nuevo: <newLink>       // si existe y es distinto de monitoredUrl
     * -- Eliminado:
     * - <líneas eliminadas>
     * link viejo: <oldLink>       // si existe
     *
     * @param monitoredUrl URL de la página que se vigila (encabezado del mail)
     * @param subjectUrl   URL destacada en el asunto (si hay link nuevo se recomienda usarlo; si no, monitoredUrl)
     * @param oldLink      primer link removido del mismo dominio (si existe; puede ser null)
     * @param summary      diff en texto (puede incluir líneas con + y -)
     */
    public void sendChangeEmail(String monitoredUrl, String subjectUrl, String oldLink, String summary) {
        String subject = "[Monitor] Cambio detectado: " + (subjectUrl != null ? subjectUrl : monitoredUrl);
        String body = buildBody(monitoredUrl, summary, subjectUrl, oldLink);
        sendEmail(subject, body);
    }

    public void sendTestEmail() {
        String subject = "[Monitor] Test SMTP";
        String body = "Hola, este es un email de prueba del Monitor.";
        sendEmail(subject, body);
    }

    // ---------------------- Formateo del cuerpo ----------------------

    private String buildBody(String monitoredUrl, String summary, String newLink, String oldLink) {
        DiffChunks chunks = parseDiff(summary);

        StringBuilder sb = new StringBuilder();
        sb.append("Página monitoreada: ").append(monitoredUrl).append("\n\n");

        sb.append("Resumen de cambios:\n");

        // Sección Añadido
        sb.append("++ Añadido:\n");
        if (!chunks.added.isEmpty()) {
            for (String line : chunks.added) {
                sb.append(prefixOnce(line, '+')).append("\n");
            }
        } else if (summary != null && !summary.isBlank()) {
            // Fallback: si no pudimos separar +/-, mostramos líneas útiles como + … (sin duplicar headers/meta)
            for (String raw : summary.split("\\R")) {
                String t = raw.strip();
                if (t.isEmpty() || isSectionHeaderOrMeta(t)) continue;
                sb.append(prefixOnce(t, '+')).append("\n");
            }
        } else {
            sb.append("(sin cambios añadidos)\n");
        }
        if (newLink != null && !newLink.isBlank() && !newLink.equals(monitoredUrl)) {
            sb.append("link nuevo: ").append(newLink).append("\n");
        }

        // Sección Eliminado
        sb.append("-- Eliminado:\n");
        if (!chunks.removed.isEmpty()) {
            for (String line : chunks.removed) {
                sb.append(prefixOnce(line, '-')).append("\n");
            }
        } else {
            sb.append("(sin cambios eliminados)\n");
        }
        if (oldLink != null && !oldLink.isBlank()) {
            sb.append("link viejo: ").append(oldLink).append("\n");
        }

        return sb.toString();
    }

    private record DiffChunks(List<String> added, List<String> removed) {}

    /**
     * Separa líneas agregadas (+) y eliminadas (-) del summary, ignorando encabezados/meta
     * como “++ Añadido:”, “-- Eliminado:”, “Resumen de cambios:”, “link nuevo: …”, etc.
     */
    private DiffChunks parseDiff(String summary) {
        List<String> plus = new ArrayList<>();
        List<String> minus = new ArrayList<>();
        if (summary == null || summary.isBlank()) {
            return new DiffChunks(plus, minus);
        }
        for (String raw : summary.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || isSectionHeaderOrMeta(line)) continue;

            if (line.startsWith("+")) {
                plus.add(line);
            } else if (line.startsWith("-")) {
                minus.add(line);
            }
            // Otras líneas sin prefijo se ignoran acá (el fallback de buildBody puede mostrarlas como '+')
        }
        return new DiffChunks(plus, minus);
    }

    /** Detecta encabezados/meta para no duplicar secciones en el cuerpo del mail. */
    private boolean isSectionHeaderOrMeta(String line) {
        String t = line.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        // Encabezados de sección típicos y variantes sin tilde
        if (t.equals("++ añadido:") || t.equals("++ anadido:") ||
                t.equals("-- eliminado:") ||
                t.equals("resumen de cambios:")) {
            return true;
        }
        // Evitar duplicar si el summary ya trae estas líneas
        if (t.startsWith("link nuevo:") || t.startsWith("link viejo:")) {
            return true;
        }
        return false;
    }

    /** Asegura un único prefijo (+/-) al inicio de cada línea. */
    private String prefixOnce(String line, char sign) {
        String t = line.strip();
        if (t.isEmpty()) return String.valueOf(sign);
        if (t.charAt(0) == sign) return t;   // ya tiene el prefijo correcto
        return sign + " " + t;
    }

    // ---------------------- Envío con SendGrid ----------------------

    private void sendEmail(String subject, String body) {
        if (props.getMailTo() == null || props.getMailTo().isEmpty()) {
            log.warn("No hay destinatarios configurados para las notificaciones.");
            return;
        }

        SendGrid sendGrid = createClient();
        if (sendGrid == null) {
            log.error("No se creó el cliente SendGrid (API Key nula o vacía).");
            return;
        }

        for (String recipient : props.getMailTo()) {
            try {
                Mail mail = new Mail(
                        new Email(props.getMailFrom()),
                        subject,
                        new Email(recipient),
                        new Content("text/plain", body)
                );

                // Deshabilitar click-tracking para que SendGrid no reescriba los links
                TrackingSettings trackingSettings = new TrackingSettings();
                ClickTrackingSetting clickTracking = new ClickTrackingSetting();
                clickTracking.setEnable(false);
                clickTracking.setEnableText(false);
                trackingSettings.setClickTrackingSetting(clickTracking);
                mail.setTrackingSettings(trackingSettings);

                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                Response response = sendGrid.api(request);
                log.info("SendGrid status={}, body={}, headers={} → destinatario={}",
                        response.getStatusCode(), response.getBody(), response.getHeaders(), recipient);
            } catch (IOException ex) {
                log.error("No se pudo enviar el correo a {}: {}", recipient, ex.getMessage(), ex);
                throw new RuntimeException("Error al enviar correo con SendGrid", ex);
            }
        }
    }

    private SendGrid createClient() {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.error("SENDGRID_API_KEY no está configurada. Configure la variable de entorno para habilitar el envío de correos.");
            return null;
        }
        return new SendGrid(sendGridApiKey);
    }
}



