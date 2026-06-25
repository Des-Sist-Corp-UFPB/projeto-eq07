package br.ufpb.dsc.corrida.audit.controller;

import br.ufpb.dsc.corrida.audit.domain.AuditLog;
import br.ufpb.dsc.corrida.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/admin/auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listAuditLogs(@RequestParam(defaultValue = "0") int page, Model model) {
        // Busca os logs ordenados do mais recente para o mais antigo, 50 por página
        Page<AuditLog> logsPage = auditLogRepository.findAll(
                PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        model.addAttribute("logsPage", logsPage);
        return "admin/auditoria";
    }

    @GetMapping("/{id}/detalhes")
    public String viewAuditLogDetails(@PathVariable String id, Model model) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (logOpt.isPresent()) {
            model.addAttribute("log", logOpt.get());
            // Retorna apenas o fragmento HTMX para preencher o Modal, sem recarregar a página
            return "admin/fragment-audit-details :: auditModalContent";
        }
        return "error/404";
    }
}
