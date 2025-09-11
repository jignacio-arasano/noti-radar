package scrapping.noti_radar.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class DiffService {

    public String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Diff simple por líneas y resumen (máx 800 chars) */
    public String summarizeDiff(String oldText, String newText, int maxChars) {
        List<String> oldLines = Arrays.asList(Optional.ofNullable(oldText).orElse("").split("\\R"));
        List<String> newLines = Arrays.asList(Optional.ofNullable(newText).orElse("").split("\\R"));

        Set<String> oldSet = new HashSet<>(oldLines);
        Set<String> newSet = new HashSet<>(newLines);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();

        for (String l : newLines) if (!oldSet.contains(l) && !l.isBlank()) added.add(l);
        for (String l : oldLines) if (!newSet.contains(l) && !l.isBlank()) removed.add(l);

        StringBuilder sb = new StringBuilder();
        if (!added.isEmpty()) sb.append("++ Añadido:\n");
        for (int i = 0; i < Math.min(5, added.size()); i++) {
            sb.append("+ ").append(truncate(added.get(i), 180)).append("\n");
        }
        if (!removed.isEmpty()) sb.append("-- Eliminado:\n");
        for (int i = 0; i < Math.min(5, removed.size()); i++) {
            sb.append("- ").append(truncate(removed.get(i), 180)).append("\n");
        }
        String out = sb.toString().trim();
        if (out.length() > maxChars) out = out.substring(0, maxChars - 3) + "...";
        return out.isBlank() ? "(Cambios menores o reordenamientos)" : out;
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
