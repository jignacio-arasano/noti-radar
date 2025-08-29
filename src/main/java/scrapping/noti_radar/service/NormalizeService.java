package scrapping.noti_radar.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class NormalizeService {

    public String extractMainText(Document doc) {
        // quitar scripts, styles, comments
        doc.select("script, style, noscript").remove();
        doc.select("header, footer, nav, aside").remove();
        removeComments(doc);

        Element main = selectMain(doc);
        String text = (main != null ? main.text() : doc.body().text());
        // normalizar espacios
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private Element selectMain(Document doc) {
        Elements candidates = new Elements();
        candidates.addAll(doc.select("article"));
        candidates.addAll(doc.select("main"));
        candidates.addAll(doc.select("[role=main]"));
        if (!candidates.isEmpty()) return candidates.first();
        // fallback: el contenedor más largo
        Element longest = null;
        int maxLen = 0;
        for (Element e : doc.select("body *")) {
            int len = e.text().length();
            if (len > maxLen) { maxLen = len; longest = e; }
        }
        return longest;
    }

    private void removeComments(Element root) {
        root.traverse(new org.jsoup.select.NodeVisitor() {
            @Override public void head(org.jsoup.nodes.Node node, int depth) {}
            @Override public void tail(org.jsoup.nodes.Node node, int depth) {
                if (node.nodeName().equals("#comment")) node.remove();
            }
        });
    }
}