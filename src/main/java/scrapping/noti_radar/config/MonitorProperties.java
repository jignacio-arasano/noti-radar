package scrapping.noti_radar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    private String userAgent;
    private int timeoutMs;
    private int intervalMinutes;
    private int jitterMsMax;
    private String seedUrls;
    private String mailFrom;
    private String mailTo;

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public int getJitterMsMax() {
        return jitterMsMax;
    }

    public void setJitterMsMax(int jitterMsMax) {
        this.jitterMsMax = jitterMsMax;
    }

    public String getSeedUrls() {
        return seedUrls;
    }

    public void setSeedUrls(String seedUrls) {
        this.seedUrls = seedUrls;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    // getters/setters...
}