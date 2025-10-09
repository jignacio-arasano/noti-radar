package scrapping.noti_radar.service;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scrapping.noti_radar.model.MonitoredPage;
import scrapping.noti_radar.model.PageVersion;
import scrapping.noti_radar.repository.MonitoredPageRepository;
import scrapping.noti_radar.repository.PageVersionRepository;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final FetchService fetchService;
    private final NormalizeService normalizeService;
    private final DiffService diffService;
    private final NotificationService notificationService;
    private final MonitoredPageRepository pageRepo;
    private final PageVersionRepository versionRepo;

    public MonitorService(FetchService fetchService,
                          NormalizeService normalizeService,
                          DiffService diffService,
                          NotificationService notificationService,
                          MonitoredPageRepository pageRepo,
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
            try { Thread.sleep((long) (Math.random() * jitterMs)); }
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            checkOne(p.getId());
        }
    }

    @Transactional
    public void checkOne(Long id) {
        MonitoredPage p = pageRepo.findById(id).orElseThrow();
        p.setLastChecked(Instant.now());

        try {
            if (!fetchService.isAllowedByRobots(p.getUrl())) {
                log.warn("Bloqueado por robots.txt: {}", p.getUrl());
                p.setLastError("Bloqueado por robots.txt");
                pageRepo.save(p);
                return;
            }

            Document doc = fetchService.fetch(p.getUrl());
            doc.setBaseUri(p.getUrl());

            String normalized = normalizeService.extractMainText(doc);
            List<String> allLinks = normalizeService.extractLinks(doc);

            // --- Filtro por sección (evita /videos, etc.) ---
            String baseHost = extractHost(p.getUrl());
            String basePrefix = extractSectionPrefix(p.getUrl()); // p.ej. "/sociedad/"
            List<String> sectionLinks = filterLinksBySection(allLinks, baseHost, basePrefix);

            // Snapshot solo de la sección
            String previousLinksRaw = p.getLastLinks();
            String linksSnapshot     = String.join("\n", sectionLinks);

            // Detección del PRIMER link agregado en la sección
            String detectedNewLink = findFirstAdded(previousLinksRaw, sectionLinks, baseHost, basePrefix);

            String hash = diffService.sha256(normalized);
            boolean changed = (p.getLastHash() == null) || !p.getLastHash().equals(hash);

            if (changed) {
                String summary = diffService.summarizeDiff(p.getLastContent(), normalized, 800);

                // Si hay link nuevo real, rotamos historial persistido (prev <- last, last <- nuevo)
                if (detectedNewLink != null && !detectedNewLink.equals(p.getLastAddedLink())) {
                    p.setPrevAddedLink(p.getLastAddedLink());
                    p.setLastAddedLink(detectedNewLink);
                }

                p.setLastHash(hash);
                p.setLastChanged(Instant.now());
                p.setLastContent(normalized);
                p.setLastError(null);
                p.setLastLinks(linksSnapshot);
                pageRepo.save(p);

                versionRepo.save(new PageVersion(p, normalized, hash));

                // Para el subject usamos el último “nuevo” persistido; si no hay, la URL monitoreada
                String subjectUrl = (p.getLastAddedLink() != null) ? p.getLastAddedLink() : p.getUrl();
                notificationService.sendChangeEmail(
                        p.getUrl(),
                        subjectUrl,           // se mostrará como "link nuevo" si es distinto de monitoredUrl
                        p.getPrevAddedLink(), // “link viejo” persistido
                        summary
                );

                log.info("Cambio detectado en {}. Hash {}", p.getUrl(), hash);
            } else {
                p.setLastError(null);
                p.setLastLinks(linksSnapshot);
                pageRepo.save(p);
                log.info("Sin cambios: {}", p.getUrl());
            }
        } catch (Exception e) {
            p.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            pageRepo.save(p);
            log.error("Error al chequear {} -> {}", p.getUrl(), e.toString());
        }
    }

    // ---------------- Sección helpers de sección ----------------

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return (host != null && host.startsWith("www.")) ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    /** Devuelve prefijo de sección normalizado, p.ej. "/sociedad/". Si no hay, "/" */
    private String extractSectionPrefix(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) return "/";
            String[] parts = path.split("/");
            for (String part : parts) {
                if (!part.isBlank()) return "/" + part + "/";
            }
            return "/";
        } catch (Exception e) {
            return "/";
        }
    }

    private boolean isSameSection(String link, String baseHost, String basePrefix) {
        try {
            URI u = URI.create(link);
            String linkHost = u.getHost();
            if (linkHost != null && linkHost.startsWith("www.")) linkHost = linkHost.substring(4);
            if (linkHost == null || baseHost == null || !linkHost.endsWith(baseHost)) return false;

            String path = u.getPath();
            if (path == null) path = "/";
            if (!path.endsWith("/")) path = path + "/";
            return path.startsWith(basePrefix);
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> filterLinksBySection(List<String> links, String baseHost, String basePrefix) {
        List<String> out = new ArrayList<>();
        if (links == null) return out;
        for (String link : links) {
            if (isSameSection(link, baseHost, basePrefix)) out.add(link);
        }
        return out;
    }

    // ------------- Detección agregado (solo para actualizar last/prev) -------------

    private String findFirstAdded(String previousLinksRaw, List<String> currentSectionLinks,
                                  String baseHost, String basePrefix) {
        Set<String> prev = toFilteredSet(previousLinksRaw, baseHost, basePrefix);
        for (String link : currentSectionLinks) {
            if (!prev.contains(link)) return link;
        }
        return null;
    }

    private Set<String> toFilteredSet(String linksRaw, String baseHost, String basePrefix) {
        Set<String> set = new LinkedHashSet<>();
        if (linksRaw == null || linksRaw.isBlank()) return set;
        for (String line : linksRaw.split("\\R")) {
            String t = line.trim();
            if (!t.isEmpty() && isSameSection(t, baseHost, basePrefix)) set.add(t);
        }
        return set;
    }
}



