package scrapping.noti_radar;

import scrapping.noti_radar.config.MonitorProperties;
import scrapping.noti_radar.model.MonitoredPage;
import scrapping.noti_radar.repository.MonitoredPageRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class NotiRadarApplication {

	// Al iniciar la app, fijamos zona horaria y locale para formateo en vistas
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Cordoba"));
		Locale.setDefault(new Locale("es", "AR"));
	}

	public static void main(String[] args) {
		SpringApplication.run(NotiRadarApplication.class, args);
	}

	@Bean
	public org.springframework.boot.CommandLineRunner seed(MonitorProperties props, MonitoredPageRepository repo) {
		return args -> {
			String s = props.getSeedUrls();
			if (s != null && !s.isBlank()) {
				for (String u : s.split(",")) {
					String url = u.trim();
					if (!url.isBlank() && repo.findByUrl(url).isEmpty()) {
						repo.save(new MonitoredPage(url));
					}
				}
			}
		};
	}
}
