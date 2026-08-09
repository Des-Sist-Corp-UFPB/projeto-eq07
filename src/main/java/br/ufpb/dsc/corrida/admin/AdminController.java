package br.ufpb.dsc.corrida.admin;

import br.ufpb.dsc.corrida.audit.AuditLogRepository;
import br.ufpb.dsc.corrida.featuretoggle.FeatureFlagRepository;
import br.ufpb.dsc.corrida.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final FeatureFlagRepository featureFlagRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLogs(Model model, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("logsPage", auditLogRepository.findAll(pageable));
        return "admin/audit";
    }

    @GetMapping("/users")
    public String users(Model model, @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        model.addAttribute("usersPage", userRepository.findAll(pageable));
        return "admin/users";
    }

    @GetMapping("/features")
    public String features(Model model, @PageableDefault(size = 20, sort = "keyName") Pageable pageable) {
        model.addAttribute("featuresPage", featureFlagRepository.findAll(pageable));
        return "admin/features";
    }
}
