package br.ufpb.dsc.corrida.admin;

import br.ufpb.dsc.corrida.audit.AuditLogRepository;
import br.ufpb.dsc.corrida.featuretoggle.FeatureFlagRepository;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController — Unit Tests")
class AdminControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    private AdminController adminController;

    @Test
    @DisplayName("dashboard() — retorna view admin/dashboard")
    void dashboard() {
        String view = adminController.dashboard();
        assertThat(view).isEqualTo("admin/dashboard");
    }

    @Test
    @DisplayName("auditLogs() — adiciona página de logs ao modelo")
    void auditLogs() {
        Model model = new ConcurrentModel();
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        String view = adminController.auditLogs(model, pageable);

        assertThat(view).isEqualTo("admin/audit");
        assertThat(model.containsAttribute("logsPage")).isTrue();
    }

    @Test
    @DisplayName("users() — adiciona página de usuários ao modelo")
    void users() {
        Model model = new ConcurrentModel();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        String view = adminController.users(model, pageable);

        assertThat(view).isEqualTo("admin/users");
        assertThat(model.containsAttribute("usersPage")).isTrue();
    }

    @Test
    @DisplayName("features() — adiciona página de feature flags ao modelo")
    void features() {
        Model model = new ConcurrentModel();
        Pageable pageable = PageRequest.of(0, 20);
        when(featureFlagRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        String view = adminController.features(model, pageable);

        assertThat(view).isEqualTo("admin/features");
        assertThat(model.containsAttribute("featuresPage")).isTrue();
    }
}
