package scrapping.noti_radar.service;

import scrapping.noti_radar.model.MonitoredPage;
import scrapping.noti_radar.model.PageVersion;
import scrapping.noti_radar.repository.MonitoredPageRepository;
import scrapping.noti_radar.repository.PageVersionRepository;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MonitorService {
    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final FetchService fetchService;
    private final NormalizeService normalizeService;
    private final DiffService diffService;
    private final NotificationService notificationService;
    private final MonitoredPageRepository pageRepo;
    private final PageVersionRepository versionRepo;

    public MonitorService(FetchService fetchService, NormalizeService normalizeService, DiffService diffService,
                          NotificationService notificationService, MonitoredPageRepository pageRepo,
                          PageVersionRepository versionRepo) {
        this.fetchService = fetchService;
        this.normalizeService = normalizeService;
        this.diffService = diffService;
        this.notificationService = notificationService;
        this.pageRepo = pageRepo;
        this.versionRepo = versionRepo;
    }

    public void checkAllWithJitter(long jitterMs) {
        List<MonitoredPage> pages = pageRepo.findAll();
        for (MonitoredPage p : pages) {
            try { Thread.sleep((long) (Math.random() * jitterMs)); } catch (InterruptedException ignored) {}
            checkOne(p.getId());
        }
    }

    @Transactional
    public void checkOne(Long id) {
        MonitoredPage p = pageRepo.findById(id).orElseThrow();
        p.setLastChecked(Instant.now());
        try {
            if (!fetchService.isAllowedByRobots(p.getUrl())) {
                String msg = "Bloqueado por robots.txt";
                log.warn("ROBOTS: {}", p.getUrl());
                p.setLastError(msg);
                pageRepo.save(p);
                return;
            }
            Document doc = fetchService.fetch(p.getUrl());
            String normalized = normalizeService.extractMainText(doc);
            String hash = diffService.sha256(normalized);

            if (p.getLastHash() == null || !p.getLastHash().equals(hash)) {
                String summary = diffService.summarizeDiff(p.getLastContent(), normalized, 800);

                p.setLastHash(hash);
                p.setLastChanged(Instant.now());
                p.setLastContent(normalized);
                p.setLastError(null);
                pageRepo.save(p);

                versionRepo.save(new PageVersion(p, normalized, hash));

                notificationService.sendChangeEmail(p.getUrl(), summary);
                log.info("Cambio detectado en {}. Hash {}", p.getUrl(), hash);
            } else {
                p.setLastError(null);
                pageRepo.save(p);
                log.info("Sin cambios: {}", p.getUrl());
            }
        } catch (Exception e) {
            p.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            pageRepo.save(p);
            log.error("Error al chequear {} -> {}", p.getUrl(), e.toString());
        }
    }
}

