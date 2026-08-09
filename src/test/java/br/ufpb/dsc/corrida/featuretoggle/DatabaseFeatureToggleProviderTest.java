package br.ufpb.dsc.corrida.featuretoggle;

import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseFeatureToggleProvider — Unit Tests")
class DatabaseFeatureToggleProviderTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private UserFeatureFlagRepository userFeatureFlagRepository;

    @Mock
    private DatabaseFeatureToggleProvider self;

    @InjectMocks
    private DatabaseFeatureToggleProvider provider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "self", self);
    }

    @Test
    @DisplayName("isEnabled() — retorna true quando ativado globalmente")
    void isEnabled_globalTrue() {
        when(self.isGlobalEnabled("TEST_FEATURE")).thenReturn(true);

        boolean enabled = provider.isEnabled("TEST_FEATURE");

        assertThat(enabled).isTrue();
        verify(userFeatureFlagRepository, never()).existsByUserIdAndFeatureName(any(), any());
    }

    @Test
    @DisplayName("isEnabled() — retorna false quando não ativado globalmente e sem usuário autenticado")
    void isEnabled_globalFalseNoAuth() {
        when(self.isGlobalEnabled("TEST_FEATURE")).thenReturn(false);

        boolean enabled = provider.isEnabled("TEST_FEATURE");

        assertThat(enabled).isFalse();
    }

    @Test
    @DisplayName("isEnabled() — retorna true quando desativado globalmente mas ativo para o usuário autenticado")
    void isEnabled_userSpecificTrue() {
        when(self.isGlobalEnabled("TEST_FEATURE")).thenReturn(false);

        User user = new User();
        user.setId(10L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null)
        );

        when(userFeatureFlagRepository.existsByUserIdAndFeatureName(10L, "TEST_FEATURE")).thenReturn(true);

        boolean enabled = provider.isEnabled("TEST_FEATURE");

        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("isGlobalEnabled() — consulta repository e retorna valor da flag")
    void isGlobalEnabled_found() {
        FeatureFlag flag = FeatureFlag.builder().keyName("FLAG1").enabled(true).build();
        when(featureFlagRepository.findByKeyName("FLAG1")).thenReturn(Optional.of(flag));

        boolean result = provider.isGlobalEnabled("FLAG1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isGlobalEnabled() — retorna false quando a flag não existe")
    void isGlobalEnabled_notFound() {
        when(featureFlagRepository.findByKeyName("NON_EXISTENT")).thenReturn(Optional.empty());

        boolean result = provider.isGlobalEnabled("NON_EXISTENT");

        assertThat(result).isFalse();
    }
}
