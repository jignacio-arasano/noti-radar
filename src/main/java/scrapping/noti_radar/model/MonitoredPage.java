package scrapping.noti_radar.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class MonitoredPage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=1024)
    private String url;

    private String lastHash;

    private Instant lastChecked;
    private Instant lastChanged;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String lastContent;

    @Column(length=1024)
    private String lastError;

    public MonitoredPage() {}
    public MonitoredPage(String url) { this.url = url; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void setLastHash(String lastHash) {
        this.lastHash = lastHash;
    }

    public Instant getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Instant lastChecked) {
        this.lastChecked = lastChecked;
    }

    public Instant getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Instant lastChanged) {
        this.lastChanged = lastChanged;
    }

    public String getLastContent() {
        return lastContent;
    }

    public void setLastContent(String lastContent) {
        this.lastContent = lastContent;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    // getters/setters...
    // (Generá con tu IDE o agrega manualmente)
    // ...
}
