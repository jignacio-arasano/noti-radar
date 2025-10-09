package scrapping.noti_radar.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NormalizeService {

    public String extractMainText(Document doc) {
        // eliminar ruido común
        doc.select("script, style, noscript").remove();
        doc.select("header, footer, nav, aside").remove();
        removeComments(doc);

        Element main = selectMain(doc);
        String text = (main != null ? main.text() : doc.body().text());
        return text.replaceAll("\\s+", " ").trim();
    }

    public List<String> extractLinks(Document doc) {
        Set<String> ordered = new LinkedHashSet<>();
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href").trim();
            if (href.isEmpty()) continue;

            String absolute = link.attr("abs:href");
            if (absolute == null || absolute.isBlank()) absolute = href;

            if (absolute.startsWith("#")) continue; // anclas internas
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

        // fallback: contenedor con más texto
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
