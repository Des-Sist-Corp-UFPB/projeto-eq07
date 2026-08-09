package br.ufpb.dsc.corrida.admin;

import br.ufpb.dsc.corrida.featuretoggle.DatabaseFeatureToggleProvider;
import br.ufpb.dsc.corrida.featuretoggle.FeatureFlag;
import br.ufpb.dsc.corrida.featuretoggle.FeatureFlagRepository;
import br.ufpb.dsc.corrida.featuretoggle.UserFeatureFlagRepository;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminApiController — Unit Tests")
class AdminApiControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private UserFeatureFlagRepository userFeatureFlagRepository;

    @Mock
    private DatabaseFeatureToggleProvider featureToggleProvider;

    @InjectMocks
    private AdminApiController adminApiController;

    @Test
    @DisplayName("toggleUserBlock() — alterna estado de bloqueio do usuário")
    void toggleUserBlock() {
        User user = new User();
        user.setId(10L);
        user.setBloqueado(false);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = adminApiController.toggleUserBlock(10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(user.getBloqueado()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("toggleFeatureGlobal() — alterna estado do feature flag global e limpa cache")
    void toggleFeatureGlobal() {
        FeatureFlag flag = FeatureFlag.builder().keyName("CREATE_RACE").enabled(true).build();
        when(featureFlagRepository.findByKeyName("CREATE_RACE")).thenReturn(Optional.of(flag));

        ResponseEntity<?> response = adminApiController.toggleFeatureGlobal("CREATE_RACE");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(flag.isEnabled()).isFalse();
        verify(featureFlagRepository).save(flag);
        verify(featureToggleProvider).evictCache("CREATE_RACE");
    }

    @Test
    @DisplayName("grantFeatureToUser() — concede flag a usuário específico")
    void grantFeatureToUser() {
        User user = new User();
        user.setId(5L);

        when(userFeatureFlagRepository.existsByUserIdAndFeatureName(5L, "SEARCH_RACES")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = adminApiController.grantFeatureToUser(5L, "SEARCH_RACES");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(userFeatureFlagRepository).save(any());
    }

    @Test
    @DisplayName("revokeFeatureFromUser() — revoga flag de usuário específico")
    void revokeFeatureFromUser() {
        ResponseEntity<?> response = adminApiController.revokeFeatureFromUser(5L, "SEARCH_RACES");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
