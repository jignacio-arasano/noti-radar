/*package scrapping.noti_radar.scheduler;

import scrapping.noti_radar.config.MonitorProperties;
import scrapping.noti_radar.service.MonitorService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class MonitorScheduler {
    private final MonitorService monitorService;
    private final MonitorProperties props;

    public MonitorScheduler(MonitorService monitorService, MonitorProperties props) {
        this.monitorService = monitorService;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "#{${monitor.intervalMinutes:15} * 60 * 1000}")
    public void run() {
        monitorService.checkAllWithJitter(props.getJitterMsMax());
    }
}*/