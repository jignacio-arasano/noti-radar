package scrapping.noti_radar.controller;

import scrapping.noti_radar.config.MonitorProperties;
import scrapping.noti_radar.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SettingsController {
    private final MonitorProperties props;
    private final NotificationService notif;

    public SettingsController(MonitorProperties props, NotificationService notif) {
        this.props = props;
        this.notif = notif;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("props", props);
        return "settings";
    }

    @PostMapping("/settings/test-email")
    public String testEmail() {
        notif.sendTestEmail();
        return "redirect:/settings?ok";
    }
}