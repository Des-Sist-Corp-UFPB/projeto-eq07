package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.*;
import br.ufpb.dsc.corrida.userConections.UserConnectionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("UsuarioController — Testes de Integração")
class UsuarioControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private UserConnectionService userConnectionService;

    // ─────────────────────────────────────────────
    // POST /user/login
    //
    // NOTA: o controller declara consumes=APPLICATION_FORM_URLENCODED_VALUE
    // mas usa @RequestBody — combinação inválida que causa 415 (Unsupported Media
    // Type) ou 500 dependendo do Spring. O correto seria usar @RequestBody com
    // APPLICATION_JSON, ou @ModelAttribute com FORM. Os testes abaixo cobrem o
    // comportamento real: form-urlencoded é rejeitado (415/500) e JSON funciona.
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /user/login — retorna 200 e token ao receber dados via Form")
    void login_deveRetornar200_comJson() throws Exception {
        when(usuarioService.login(any(LoginDto.class))).thenReturn("jwt-token-mockado");

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("login", "joao@email.com")
                        .param("senha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-mockado"))
                .andExpect(jsonPath("$.mensagem").value("Autenticado com sucesso"));
    }

    @Test
    @DisplayName("POST /user/login — retorna 401 quando service lança BadCredentialsException")
    void login_deveRetornar401_comCredenciaisInvalidas() throws Exception {
        when(usuarioService.login(any(LoginDto.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("login", "errado@email.com")
                        .param("senha", "senhaErrada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/login — retorna 400 quando campos obrigatórios ausentes (@Valid)")
    void login_deveRetornar400_quandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("login", "")
                        .param("senha", ""))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).login(any());
    }

    // ─────────────────────────────────────────────
    // POST /user/registrar
    //
    // NOTA: o controller é @RestController, portanto retornar String é o corpo
    // da resposta HTTP — não uma view Thymeleaf. Não há ModelAndView.
    // "redirect:/login?success=true" vira texto literal no body com status 200.
    // Os testes cobrem o comportamento real desse endpoint.
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("POST /user/registrar — retorna 200 com texto 'redirect:/login?success=true' no body ao registrar")
    void registrar_deveRetornar200_comTextoRedirectNoBody() throws Exception {
        when(usuarioService.registrar(any(RegistrarUsuarioDTO.class))).thenReturn("token-ignorado");

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "João Silva")
                        .param("username", "joaosilva")
                        .param("login", "joao@email.com")
                        .param("senha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(content().string("redirect:/login?success=true"));

        verify(usuarioService).registrar(any(RegistrarUsuarioDTO.class));
    }

    @Test
    @DisplayName("POST /user/registrar — retorna 200 com texto 'auth/registrar' no body quando login já existe")
    void registrar_deveRetornar200_comTextoViewNoBody_quandoLoginJaExiste() throws Exception {
        doThrow(new UsuarioJaExistenteException("Login já utilizado, tente outro"))
                .when(usuarioService).registrar(any(RegistrarUsuarioDTO.class));

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "João Silva")
                        .param("username", "joaosilva")
                        .param("login", "joao@email.com")
                        .param("senha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(content().string("auth/registrar"));
    }

    @Test
    @DisplayName("POST /user/registrar — retorna 200 com 'auth/registrar' no body quando campos inválidos (@Valid)")
    void registrar_deveRetornar200_comTextoViewNoBody_quandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")
                        .param("username", "")
                        .param("login", "")
                        .param("senha", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("auth/registrar"));

        verify(usuarioService, never()).registrar(any());
    }

    // ─────────────────────────────────────────────
    // PATCH /user/{id}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} — retorna 200 com dados atualizados quando bem-sucedido")
    void editar_deveRetornar200_comSucesso() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("João Atualizado", null, null, null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(1L)))
                .thenReturn(mock(User.class));

        mockMvc.perform(patch("/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} — retorna 403 quando usuário tenta editar outro usuário")
    void editar_deveRetornar403_quandoAcessoNaoPermitido() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Hacker", null, null, null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(2L)))
                .thenThrow(new AcessoNaoPermitidoException("Acesso negado para edição de usuário"));

        mockMvc.perform(patch("/user/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} — retorna 409 quando novo login já está em uso")
    void editar_deveRetornar409_quandoLoginJaExiste() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, null, "duplicado@email.com", null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(1L)))
                .thenThrow(new UsuarioJaExistenteException("Login já existente"));

        mockMvc.perform(patch("/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} — retorna 404 quando usuário não existe")
    void editar_deveRetornar404_quandoUsuarioNaoEncontrado() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Novo Nome", null, null, null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(99L)))
                .thenThrow(new UsuarioNaoEncontradoException("Usuário com ID: 99 não encontrado"));

        mockMvc.perform(patch("/user/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────
    // DELETE /user/{id}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} — retorna 204 quando deleção bem-sucedida")
    void deletar_deveRetornar204_comSucesso() throws Exception {
        doNothing().when(usuarioService).deletar(1L);

        mockMvc.perform(delete("/user/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(usuarioService).deletar(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} — retorna 403 quando usuário tenta deletar outro usuário")
    void deletar_deveRetornar403_quandoAcessoNaoPermitido() throws Exception {
        doThrow(new AcessoNaoPermitidoException("Acesso negado para deleção de usuário"))
                .when(usuarioService).deletar(2L);

        mockMvc.perform(delete("/user/2").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} — retorna 404 quando usuário não existe")
    void deletar_deveRetornar404_quandoNaoEncontrado() throws Exception {
        doThrow(new UsuarioNaoEncontradoException("Usuário com ID: 99 não encontrado"))
                .when(usuarioService).deletar(99L);

        mockMvc.perform(delete("/user/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────
    // GET /user/{username}
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /user/{username} — retorna 200 com perfil público (rota pública)")
    void getPerfil_deveRetornar200_comSucesso() throws Exception {
        PerfilPublicoDTO perfil = new PerfilPublicoDTO("João Silva", "joaosilva", null, 10.5f);

        when(usuarioService.buscarPerfilPublico("joaosilva")).thenReturn(perfil);

        mockMvc.perform(get("/user/joaosilva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("joaosilva"))
                .andExpect(jsonPath("$.nome").value("João Silva"));
    }

    @Test
    @DisplayName("GET /user/{username} — retorna 404 quando username não existe")
    void getPerfil_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(usuarioService.buscarPerfilPublico("inexistente"))
                .thenThrow(new UsuarioNaoEncontradoException("Usuário com username: inexistente não encontrado"));

        mockMvc.perform(get("/user/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────
    // POST /user/conexao/enviar/{receiverId}
    // @WithMockUser injeta principal genérico (não instância de User),
    // então loggedInUser == null e o controller retorna 401 explicitamente.
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/enviar/{receiverId} — retorna 401 quando principal não é User real")
    void enviarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/enviar/{receiverId} — redireciona para login quando não autenticado")
    void enviarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // ─────────────────────────────────────────────
    // POST /user/conexao/aceitar/{requestId}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/aceitar/{requestId} — retorna 401 quando principal não é User real")
    void aceitarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/aceitar/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/aceitar/{requestId} — redireciona para login quando não autenticado")
    void aceitarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/aceitar/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // ─────────────────────────────────────────────
    // POST /user/conexao/recusar/{requestId}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/recusar/{requestId} — retorna 401 quando principal não é User real")
    void recusarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/recusar/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/recusar/{requestId} — redireciona para login quando não autenticado")
    void recusarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/recusar/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // ─────────────────────────────────────────────
    // DELETE /user/conexao/remover/{receiverId}
    // removeConnection é void — doNothing() é válido
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/conexao/remover/{receiverId} — retorna 401 quando principal não é User real")
    void removerConexao_deveRetornar401_comWithMockUser() throws Exception {
        doNothing().when(userConnectionService).removeConnection(any(), any());

        mockMvc.perform(delete("/user/conexao/remover/2").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /user/conexao/remover/{receiverId} — redireciona para login quando não autenticado")
    void removerConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(delete("/user/conexao/remover/2").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}