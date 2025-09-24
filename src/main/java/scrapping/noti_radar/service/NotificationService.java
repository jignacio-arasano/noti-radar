package scrapping.noti_radar.service;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import scrapping.noti_radar.config.MonitorProperties;


@Service
public class NotificationService {
    //private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MonitorProperties props;
    private final String sendGridApiKey;

    /*public NotificationService(JavaMailSender mailSender, MonitorProperties props) {
        this.mailSender = mailSender;*/
    public NotificationService(MonitorProperties props, @Value("${SENDGRID_API_KEY:}") String sendGridApiKey) {
            this.props = props;
            this.sendGridApiKey = sendGridApiKey;
        }

        public void sendChangeEmail(String url, String summary) {
            /*SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getMailFrom());
            //msg.setTo(props.getMailTo());
            msg.setTo(props.getMailTo().toArray(new String[0]));
            msg.setSubject("[Monitor] Cambio detectado: " + url);
            msg.setText("URL: " + url + "\n\nResumen de cambios:\n" + summary + "\n");
            mailSender.send(msg);*/
            String subject = "[Monitor] Cambio detectado: " + url;
            String body = "URL: " + url + "\n\nResumen de cambios:\n" + summary + "\n";
            sendEmail(subject, body);
        }

        public void sendTestEmail() {
            /*SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getMailFrom());
            //msg.setTo(props.getMailTo());
            msg.setTo(props.getMailTo().toArray(new String[0]));
            msg.setSubject("[Monitor] Test SMTP");
            msg.setText("Hola, este es un email de prueba del Monitor.");
            mailSender.send(msg);*/
            String subject = "[Monitor] Test SMTP";
            String body = "Hola, este es un email de prueba del Monitor.";
            sendEmail(subject, body);
        }

    private void sendEmail(String subject, String body) {
        if (props.getMailTo() == null || props.getMailTo().isEmpty()) {
            log.warn("No hay destinatarios configurados para las notificaciones.");
            return;
        }

        // Debug extra: mostrar config básica
        log.info("Intentando enviar email...");
        log.info("Remitente configurado: {}", props.getMailFrom());
        log.info("Destinatarios: {}", props.getMailTo());
        log.info("Asunto: {}", subject);
        log.info("Longitud del cuerpo: {} caracteres", body.length());
        log.info("API Key cargada: {}",
                (sendGridApiKey != null && !sendGridApiKey.isBlank())
                        ? "Sí (longitud " + sendGridApiKey.length() + ")"
                        : "NO ❌");

        SendGrid sendGrid = createClient();
        if (sendGrid == null) {
            log.error("No se creó el cliente SendGrid (API Key nula o vacía).");
            return;
        }

        for (String recipient : props.getMailTo()) {
            try {
                // Crear correo con helpers de SendGrid
                Mail mail = new Mail(
                        new Email(props.getMailFrom()),
                        subject,
                        new Email(recipient),
                        new Content("text/plain", body)
                );

                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                // Llamada a la API de SendGrid
                Response response = sendGrid.api(request);

                // Log completo de la respuesta
                log.info("SendGrid status={}, body={}, headers={} → destinatario={}",
                        response.getStatusCode(),
                        response.getBody(),
                        response.getHeaders(),
                        recipient);

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
