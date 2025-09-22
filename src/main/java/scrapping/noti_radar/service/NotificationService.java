package scrapping.noti_radar.service;

import scrapping.noti_radar.config.MonitorProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final JavaMailSender mailSender;
    private final MonitorProperties props;

    public NotificationService(JavaMailSender mailSender, MonitorProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendChangeEmail(String url, String summary) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.getMailFrom());
        //msg.setTo(props.getMailTo());
        msg.setTo(props.getMailTo().toArray(new String[0]));
        msg.setSubject("[Monitor] Cambio detectado: " + url);
        msg.setText("URL: " + url + "\n\nResumen de cambios:\n" + summary + "\n");
        mailSender.send(msg);
    }

    public void sendTestEmail() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.getMailFrom());
        //msg.setTo(props.getMailTo());
        msg.setTo(props.getMailTo().toArray(new String[0]));
        msg.setSubject("[Monitor] Test SMTP");
        msg.setText("Hola, este es un email de prueba del Monitor.");
        mailSender.send(msg);
    }
}
