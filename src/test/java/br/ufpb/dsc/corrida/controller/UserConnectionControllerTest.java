package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserConnectionService;
import br.ufpb.dsc.corrida.user.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserConnection REST Endpoints — Integration Tests")
class UserConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserConnectionService userConnectionService;

    @MockBean
    private UsuarioService usuarioService;

    private User loggedInUser;

    @BeforeEach
    void setUp() {
        loggedInUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(loggedInUser, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(loggedInUser, "nome", "Logged In User");
        org.springframework.test.util.ReflectionTestUtils.setField(loggedInUser, "login", "user@test.com");
    }

    @Test
    @DisplayName("POST /user/conexao/enviar/{receiverId} — should invoke sendConnectionRequest and return 200 OK")
    void enviarConexao_shouldReturn200() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userConnectionService).sendConnectionRequest(1L, 2L);
    }

    @Test
    @DisplayName("POST /user/conexao/aceitar/{requestId} — should invoke acceptConnectionRequest and return 200 OK")
    void aceitarConexao_shouldReturn200() throws Exception {
        mockMvc.perform(post("/user/conexao/aceitar/10")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userConnectionService).acceptConnectionRequest(10L, 1L);
    }

    @Test
    @DisplayName("POST /user/conexao/recusar/{requestId} — should invoke declineConnectionRequest and return 200 OK")
    void recusarConexao_shouldReturn200() throws Exception {
        mockMvc.perform(post("/user/conexao/recusar/10")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userConnectionService).declineConnectionRequest(10L, 1L);
    }

    @Test
    @DisplayName("DELETE /user/conexao/remover/{receiverId} — should invoke removeConnection and return 200 OK")
    void removerConexao_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/user/conexao/remover/2")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userConnectionService).removeConnection(1L, 2L);
    }

    @Test
    @DisplayName("POST /user/conexao/enviar/{receiverId} — should return 401 Unauthorized when not authenticated")
    void enviarConexao_unauthorized() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
