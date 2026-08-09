package br.ufpb.dsc.corrida.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/auditoria")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarAuditoria(Model model) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("logs", logs);
        return "admin/auditoria";
    }
}
