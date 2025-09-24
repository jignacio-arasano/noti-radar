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
            SendGrid sendGrid = createClient();
            if (sendGrid == null) {
                return;
            }
            for (String recipient : props.getMailTo()) {
                Mail mail = new Mail(new Email(props.getMailFrom()), subject, new Email(recipient), new Content("text/plain", body));
                Request request = new Request();
                try {
                    request.setMethod(Method.POST);
                    request.setEndpoint("mail/send");
                    request.setBody(mail.build());
                    Response response = sendGrid.api(request);
                    log.debug("SendGrid respondió {} al enviar correo a {}", response.getStatusCode(), recipient);
                } catch (IOException ex) {
                    log.error("No se pudo enviar el correo a {}: {}", recipient, ex.getMessage());
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
