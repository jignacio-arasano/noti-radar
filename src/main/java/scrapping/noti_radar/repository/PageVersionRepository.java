package scrapping.noti_radar.repository;

import scrapping.noti_radar.model.PageVersion;
import scrapping.noti_radar.model.MonitoredPage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {
    List<PageVersion> findByPageOrderByCreatedAtDesc(MonitoredPage page);
    @Modifying
    @Transactional
    int deleteByPage(MonitoredPage page);
}
