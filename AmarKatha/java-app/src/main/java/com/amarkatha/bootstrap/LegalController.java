package com.amarkatha.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/legal")
public class LegalController {

    private final String contactEmail;
    private final String grievanceOfficerName;

    public LegalController(
            @Value("${amarkatha.contact-email:hello@amarkatha.in}") String contactEmail,
            @Value("${amarkatha.grievance.officer-name:Founder (interim)}") String grievanceOfficerName
    ) {
        this.contactEmail = contactEmail;
        this.grievanceOfficerName = grievanceOfficerName;
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/legal/terms";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        addCommon(model);
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        addCommon(model);
        return "legal/privacy";
    }

    @GetMapping("/grievance")
    public String grievance(Model model) {
        addCommon(model);
        return "legal/grievance";
    }

    private void addCommon(Model model) {
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("grievanceOfficerName", grievanceOfficerName);
    }
}
