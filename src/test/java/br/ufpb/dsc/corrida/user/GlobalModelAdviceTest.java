package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.userConections.UserConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalModelAdvice — Testes Unitários do ControllerAdvice")
class GlobalModelAdviceTest {

    @Mock
    private UserConnectionService userConnectionService;

    @Mock
    private OrganizerService organizerService;

    @Mock
    private UserInfoService userInfoService;

    @InjectMocks
    private GlobalModelAdvice globalModelAdvice;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("teste_user");
    }

    @Test
    @DisplayName("Deve retornar a URL da foto quando usuário logado possuir perfil e foto")
    void addUserProfilePhotoUrl_ComFoto_DeveRetornarUrl() {
        UserInfoRespostaDTO userInfoMock = new UserInfoRespostaDTO(
                1L, 1L, 70f, 1.75f, null, 10f, null, 
                "http://minio/bucket/foto.png", 
                null, null, true, "00000000000", null, null
        );
        when(userInfoService.buscarPorUsuarioId(1L)).thenReturn(userInfoMock);

        String result = globalModelAdvice.addUserProfilePhotoUrl(mockUser);

        assertEquals("http://minio/bucket/foto.png", result);
    }

    @Test
    @DisplayName("Deve retornar null quando userInfoService falhar ou perfil não tiver foto")
    void addUserProfilePhotoUrl_ErroAoBuscar_DeveRetornarNull() {
        when(userInfoService.buscarPorUsuarioId(1L)).thenThrow(new RuntimeException("Perfil não encontrado"));

        String result = globalModelAdvice.addUserProfilePhotoUrl(mockUser);

        assertNull(result);
    }

    @Test
    @DisplayName("Deve retornar null quando usuário não estiver logado")
    void addUserProfilePhotoUrl_SemUsuarioLogado_DeveRetornarNull() {
        String result = globalModelAdvice.addUserProfilePhotoUrl(null);

        assertNull(result);
    }

    @Test
    @DisplayName("Deve retornar contagem de solicitações pendentes")
    void addPendingRequestsCount_ComUsuario_DeveRetornarContagem() {
        when(userConnectionService.getPendingRequestsCount(1L)).thenReturn(5L);

        Long count = globalModelAdvice.addPendingRequestsCount(mockUser);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Deve retornar 0 solicitações se usuário for nulo")
    void addPendingRequestsCount_SemUsuario_DeveRetornarZero() {
        Long count = globalModelAdvice.addPendingRequestsCount(null);

        assertEquals(0L, count);
    }
}
