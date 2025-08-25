package scrapping.noti_radar.model;

import jakarta.persistence.*;
import java.time.Instant;



@Entity
public class PageVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private MonitoredPage page;

    private Instant createdAt;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    private String hash;

    public PageVersion() {}
    public PageVersion(MonitoredPage page, String content, String hash) {
        this.page = page;
        this.content = content;
        this.hash = hash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MonitoredPage getPage() {
        return page;
    }

    public void setPage(MonitoredPage page) {
        this.page = page;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // getters/setters...
}
