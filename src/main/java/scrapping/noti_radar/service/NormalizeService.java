package scrapping.noti_radar.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NormalizeService {

    public String extractMainText(Document doc) {
        // eliminar ruido común
        doc.select("script, style, noscript").remove();
        doc.select("header, footer, nav, aside").remove();
        removeComments(doc);

        Element main = selectMain(doc);

        // Manejo específico para las páginas de sección de Infobae para extraer solo titulares
        if (main != null && doc.baseUri().contains("infobae.com") && main.tagName().equals("article")) {
            List<String> headlines = new ArrayList<>();
            Elements storyCards = main.children();

            for (Element card : storyCards) {
                Element headlineElement = card.selectFirst("h2");
                String headlineText;

                if (headlineElement != null) {
                    headlineText = headlineElement.text();
                } else {
                    headlineText = card.text();
                }

                if (headlineText != null && !headlineText.isBlank()) {
                    headlines.add(headlineText.replaceAll("\\s+", " ").trim());
                }
            }

            if (!headlines.isEmpty()) {
                return headlines.stream().distinct().collect(Collectors.joining("\n"));
            }
        }

        String text = (main != null ? main.text() : doc.body().text());
        return text.replaceAll("\\s+", " ").trim();
    }

    public List<String> extractLinks(Document doc) {
        Set<String> ordered = new LinkedHashSet<>();
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href").trim();
            if (href.isEmpty() || href.startsWith("#")) continue;

            String absolute = link.attr("abs:href");
            if (absolute == null || absolute.isBlank()) absolute = href;

            // Eliminar fragmentos/anclas de la URL
            if (absolute.contains("#")) {
                absolute = absolute.substring(0, absolute.indexOf('#'));
            }
            if(absolute.isEmpty()) continue;

            ordered.add(absolute);
        }
        return new ArrayList<>(ordered);
    }

    private Element selectMain(Document doc) {
        Elements candidates = new Elements();
        candidates.addAll(doc.select("article"));
        candidates.addAll(doc.select("main"));
        candidates.addAll(doc.select("[role=main]"));
        if (!candidates.isEmpty()) return candidates.first();

        Element longest = null;
        int maxLen = 0;
        for (Element e : doc.select("body *")) {
            int len = e.text().length();
            if (len > maxLen) {
                maxLen = len;
                longest = e;
            }
        }
        return longest;
    }

    private void removeComments(Element root) {
        root.traverse(new org.jsoup.select.NodeVisitor() {
            @Override public void head(org.jsoup.nodes.Node node, int depth) { }
            @Override public void tail(org.jsoup.nodes.Node node, int depth) {
                if (node.nodeName().equals("#comment")) node.remove();
            }
        });
    }
}
