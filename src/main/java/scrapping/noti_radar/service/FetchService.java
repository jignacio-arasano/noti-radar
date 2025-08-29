package scrapping.noti_radar.service;



import scrapping.noti_radar.config.MonitorProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Service
public class FetchService {
    private final MonitorProperties props;

    public FetchService(MonitorProperties props) {
        this.props = props;
    }

    public Document fetch(String url) throws IOException {
        // Rate-limit será por scheduler. Aquí: UA propio + timeout + no seguir redirecciones eternas
        Connection conn = Jsoup.connect(url)
                .userAgent(props.getUserAgent())
                .timeout(props.getTimeoutMs())
                .followRedirects(true);
        return conn.get();
    }

    // Chequeo simple de robots.txt (básico, MVP)
    public boolean isAllowedByRobots(String url) {
        try {
            URI u = URI.create(url);
            String robotsUrl = u.getScheme() + "://" + u.getHost() + "/robots.txt";
            String txt = Jsoup.connect(robotsUrl)
                    .userAgent(props.getUserAgent())
                    .timeout(props.getTimeoutMs())
                    .ignoreContentType(true)
                    .get().text();
            String path = u.getPath();
            boolean inStar = false;
            boolean allowed = true;
            for (String line : txt.split("\n")) {
                line = line.trim();
                if (line.toLowerCase().startsWith("user-agent:")) {
                    inStar = line.substring(11).trim().equals("*");
                } else if (inStar && line.toLowerCase().startsWith("disallow:")) {
                    String dis = line.substring(9).trim();
                    if (!dis.isEmpty() && path.startsWith(dis)) {
                        allowed = false;
                    }
                }
            }
            return allowed;
        } catch (Exception e) {
            // Si falla robots, preferimos ser conservadores: permitir pero loggear.
            return true;
        }
    }
}
