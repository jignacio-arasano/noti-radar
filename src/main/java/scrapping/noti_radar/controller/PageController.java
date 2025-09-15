package scrapping.noti_radar.controller;

import scrapping.noti_radar.model.MonitoredPage;
import scrapping.noti_radar.model.PageVersion;
import scrapping.noti_radar.repository.MonitoredPageRepository;
import scrapping.noti_radar.repository.PageVersionRepository;
import scrapping.noti_radar.service.MonitorService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller @Validated
public class PageController {

    private final MonitoredPageRepository pageRepo;
    private final PageVersionRepository versionRepo;
    private final MonitorService monitorService;

    public PageController(MonitoredPageRepository pageRepo, PageVersionRepository versionRepo, MonitorService monitorService) {
        this.pageRepo = pageRepo;
        this.versionRepo = versionRepo;
        this.monitorService = monitorService;
    }

    @GetMapping({"/","/pages"})
    public String list(Model model) {
        model.addAttribute("pages", pageRepo.findAll());
        return "pages";
    }

    @GetMapping("/pages/new")
    public String newForm() { return "pages_new"; }

    @PostMapping("/pages")
    public String create(@RequestParam @NotBlank String url) {
        pageRepo.findByUrl(url).orElseGet(() -> pageRepo.save(new MonitoredPage(url)));
        return "redirect:/pages";
    }

    @PostMapping("/pages/{id}/delete")
    @org.springframework.transaction.annotation.Transactional
    public String delete(@PathVariable Long id) {
        MonitoredPage p = pageRepo.findById(id).orElseThrow();
        versionRepo.deleteByPage(p);  // borra hijos primero
        pageRepo.delete(p);           // ahora sí, borra el padre
        return "redirect:/pages";
    }


    @PostMapping("/pages/{id}/check")
    public String check(@PathVariable Long id) {
        monitorService.checkOne(id);
        return "redirect:/pages";
    }

    @GetMapping("/pages/{id}/versions")
    public String versions(@PathVariable Long id, Model model) {
        MonitoredPage p = pageRepo.findById(id).orElseThrow();
        List<PageVersion> versions = versionRepo.findByPageOrderByCreatedAtDesc(p);
        model.addAttribute("page", p);
        model.addAttribute("versions", versions);
        return "pages_versions";
    }

    @GetMapping("/pages/{id}/diff/{versionId}")
    public String diff(@PathVariable Long id, @PathVariable Long versionId, Model model) {
        MonitoredPage p = pageRepo.findById(id).orElseThrow();
        PageVersion v = versionRepo.findById(versionId).orElseThrow();
        model.addAttribute("page", p);
        model.addAttribute("version", v);
        model.addAttribute("currentContent", p.getLastContent());
        model.addAttribute("versionContent", v.getContent());
        return "pages_diff";
    }
}

