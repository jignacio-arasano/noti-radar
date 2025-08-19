package scrapping.noti_radar.repository;

import scrapping.noti_radar.model.MonitoredPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonitoredPageRepository extends JpaRepository<MonitoredPage, Long> {
    Optional<MonitoredPage> findByUrl(String url);
}
