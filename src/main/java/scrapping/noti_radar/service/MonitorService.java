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
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final FetchService fetchService;
    private final NormalizeService normalizeService;
    private final DiffService diffService;
    private final NotificationService notificationService;
    private final MonitoredPageRepository pageRepo;
    private final PageVersionRepository versionRepo;
    private static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");


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

            String hash = diffService.sha256(normalized);
            boolean changed = (p.getLastHash() == null) || !p.getLastHash().equals(hash);

            if (changed) {
                String summary = diffService.summarizeDiff(p.getLastContent(), normalized, 800);

                // --- Nueva Lógica de Detección de Enlace por Palabras Clave ---
                String detectedNewLink = null;
                String firstNewTitle = findFirstNewTitle(summary);

                if (firstNewTitle != null) {
                    detectedNewLink = findBestLinkForTitle(firstNewTitle, allLinks);
                }

                // --- Fallback a la lógica anterior si no se encuentra un enlace ---
                if (detectedNewLink == null) {
                    log.warn("No se encontró un enlace para el título: '{}'. Usando método de fallback.", firstNewTitle);
                    String baseHost = extractHost(p.getUrl());
                    String basePrefix = extractSectionPrefix(p.getUrl());
                    String previousLinksRaw = p.getLastLinks();
                    List<String> sectionLinks = filterLinksBySection(allLinks, baseHost, basePrefix);
                    detectedNewLink = findFirstAdded(previousLinksRaw, sectionLinks, baseHost, basePrefix);
                }

                if (detectedNewLink != null && !detectedNewLink.equals(p.getLastAddedLink())) {
                    p.setPrevAddedLink(p.getLastAddedLink());
                    p.setLastAddedLink(detectedNewLink);
                }

                p.setLastHash(hash);
                p.setLastChanged(Instant.now());
                p.setLastContent(normalized);
                p.setLastError(null);
                p.setLastLinks(String.join("\n", allLinks));
                pageRepo.save(p);

                versionRepo.save(new PageVersion(p, normalized, hash));

                String subjectUrl = (p.getLastAddedLink() != null) ? p.getLastAddedLink() : p.getUrl();
                notificationService.sendChangeEmail(
                        p.getUrl(),
                        subjectUrl,
                        p.getPrevAddedLink(),
                        summary
                );

                log.info("Cambio detectado en {}. Hash {}", p.getUrl(), hash);
            } else {
                p.setLastError(null);
                p.setLastLinks(String.join("\n", allLinks));
                pageRepo.save(p);
                log.info("Sin cambios: {}", p.getUrl());
            }
        } catch (Exception e) {
            p.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            pageRepo.save(p);
            log.error("Error al chequear {} -> {}", p.getUrl(), e.toString());
        }
    }

    private String findFirstNewTitle(String summary) {
        if (summary == null || summary.isBlank()) return null;
        for (String line : summary.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("+ ")) {
                return trimmed.substring(2).trim();
            }
        }
        return null;
    }

    private String findBestLinkForTitle(String title, List<String> links) {
        if (title == null || title.isBlank()) return null;

        List<String> keywords = extractKeywords(title);
        if (keywords.isEmpty()) return null;

        for (String link : links) {
            long matchCount = keywords.stream()
                    .filter(kw -> link.toLowerCase().contains(kw))
                    .count();

            if (matchCount >= 2) {
                log.info("Enlace encontrado para el título '{}' con {} coincidencias: {}", title, matchCount, link);
                return link;
            }
        }

        log.warn("No se encontró un enlace con al menos 2 palabras clave para el título: {}", title);
        return null;
    }

    private List<String> extractKeywords(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String slug = DIACRITICS_AND_FRIENDS.matcher(normalized).replaceAll("");

        return Arrays.stream(slug.toLowerCase().trim().split("\\s+"))
                .map(word -> word.replaceAll("[^a-z0-9]", ""))
                .filter(word -> word.length() > 3) // Ignorar palabras muy cortas
                .collect(Collectors.toList());
    }


    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return (host != null && host.startsWith("www.")) ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractSectionPrefix(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) return "/";
            String[] parts = path.split("/");
            if (parts.length > 1) {
                return "/" + parts[1] + "/";
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



