package scrapping.noti_radar.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "monitored_page")
public class MonitoredPage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=1024)
    private String url;

    @Column(length=128)
    private String lastHash;

    private Instant lastChecked;
    private Instant lastChanged;

    @Lob @Column(columnDefinition = "CLOB")
    private String lastContent;

    @Column(length=1024)
    private String lastError;

    /** Snapshot de links (uno por línea) solo de la sección monitoreada */
    @Lob @Column(columnDefinition = "CLOB")
    private String lastLinks;

    /** Nuevo: último link detectado como “agregado” en esta sección */
    @Column(length = 1024)
    private String lastAddedLink;

    /** Nuevo: el link agregado anterior (para mostrar como “link viejo”) */
    @Column(length = 1024)
    private String prevAddedLink;

    public MonitoredPage() { }
    public MonitoredPage(String url) { this.url = url; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLastHash() { return lastHash; }
    public void setLastHash(String lastHash) { this.lastHash = lastHash; }

    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }

    public Instant getLastChanged() { return lastChanged; }
    public void setLastChanged(Instant lastChanged) { this.lastChanged = lastChanged; }

    public String getLastContent() { return lastContent; }
    public void setLastContent(String lastContent) { this.lastContent = lastContent; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getLastLinks() { return lastLinks; }
    public void setLastLinks(String lastLinks) { this.lastLinks = lastLinks; }

    public String getLastAddedLink() { return lastAddedLink; }
    public void setLastAddedLink(String lastAddedLink) { this.lastAddedLink = lastAddedLink; }

    public String getPrevAddedLink() { return prevAddedLink; }
    public void setPrevAddedLink(String prevAddedLink) { this.prevAddedLink = prevAddedLink; }
}


