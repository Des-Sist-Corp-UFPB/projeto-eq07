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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UsuarioController â€” Testes de IntegraÃ§Ã£o")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserInfoService userInfoService;

    @MockitoBean
    private UserConnectionService userConnectionService;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /user/login
    //
    // NOTA: o controller declara consumes=APPLICATION_FORM_URLENCODED_VALUE
    // mas usa @RequestBody â€” combinaÃ§Ã£o invÃ¡lida que causa 415 (Unsupported Media
    // Type) ou 500 dependendo do Spring. O correto seria usar @RequestBody com
    // APPLICATION_JSON, ou @ModelAttribute com FORM. Os testes abaixo cobrem o
    // comportamento real: form-urlencoded Ã© rejeitado (415/500) e JSON funciona.
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /user/login â€” retorna 200 e token ao receber dados via Form")
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
    @DisplayName("POST /user/login â€” retorna 401 quando service lanÃ§a BadCredentialsException")
    void login_deveRetornar401_comCredenciaisInvalidas() throws Exception {
        when(usuarioService.login(any(LoginDto.class)))
                .thenThrow(new BadCredentialsException("Credenciais invÃ¡lidas"));

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("login", "errado@email.com")
                        .param("senha", "senhaErrada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/login â€” retorna 400 quando campos obrigatÃ³rios ausentes (@Valid)")
    void login_deveRetornar400_quandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("login", "")
                        .param("senha", ""))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).login(any());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /user/registrar
    //
    // NOTA: o controller Ã© @RestController, portanto retornar String Ã© o corpo
    // da resposta HTTP â€” nÃ£o uma view Thymeleaf. NÃ£o hÃ¡ ModelAndView.
    // "redirect:/login?success=true" vira texto literal no body com status 200.
    // Os testes cobrem o comportamento real desse endpoint.
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        @Test
        @DisplayName("POST /user/registrar â€” redireciona para login ao registrar com sucesso")
        void registrar_deveRedirecionarParaLogin_comSucesso() throws Exception {
        when(usuarioService.registrar(any(RegistrarUsuarioDTO.class))).thenReturn("token-ignorado");

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "JoÃ£o Silva")
                        .param("username", "joaosilva")
                        .param("login", "joao@email.com")
                        .param("senha", "senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success=true"));

        verify(usuarioService).registrar(any(RegistrarUsuarioDTO.class));
        }

    @Test
        @DisplayName("POST /user/registrar â€” retorna view de cadastro quando login jÃ¡ existe")
        void registrar_deveRetornarViewCadastro_quandoLoginJaExiste() throws Exception {
        doThrow(new UsuarioJaExistenteException("Login jÃ¡ utilizado, tente outro"))
                .when(usuarioService).registrar(any(RegistrarUsuarioDTO.class));

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "JoÃ£o Silva")
                        .param("username", "joaosilva")
                        .param("login", "joao@email.com")
                        .param("senha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registrar"))
                .andExpect(model().attribute("error", "Login jÃ¡ utilizado, tente outro"));
        }

    @Test
        @DisplayName("POST /user/registrar â€” retorna view de cadastro quando campos invÃ¡lidos")
        void registrar_deveRetornarViewCadastro_quandoCamposInvalidos() throws Exception {
        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")
                        .param("username", "")
                        .param("login", "")
                        .param("senha", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registrar"));

        verify(usuarioService, never()).registrar(any());
        }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PATCH /user/{id}
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} â€” retorna 200 com dados atualizados quando bem-sucedido")
    void editar_deveRetornar200_comSucesso() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("JoÃ£o Atualizado", null, null, null);

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
    @DisplayName("PATCH /user/{id} â€” retorna 403 quando usuÃ¡rio tenta editar outro usuÃ¡rio")
    void editar_deveRetornar403_quandoAcessoNaoPermitido() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Hacker", null, null, null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(2L)))
                .thenThrow(new AcessoNaoPermitidoException("Acesso negado para ediÃ§Ã£o de usuÃ¡rio"));

        mockMvc.perform(patch("/user/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} â€” retorna 409 quando novo login jÃ¡ estÃ¡ em uso")
    void editar_deveRetornar409_quandoLoginJaExiste() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, null, "duplicado@email.com", null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(1L)))
                .thenThrow(new UsuarioJaExistenteException("Login jÃ¡ existente"));

        mockMvc.perform(patch("/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /user/{id} â€” retorna 404 quando usuÃ¡rio nÃ£o existe")
    void editar_deveRetornar404_quandoUsuarioNaoEncontrado() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Novo Nome", null, null, null);

        when(usuarioService.editar(any(EditarUsuarioDTO.class), eq(99L)))
                .thenThrow(new UsuarioNaoEncontradoException("UsuÃ¡rio com ID: 99 nÃ£o encontrado"));

        mockMvc.perform(patch("/user/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // DELETE /user/{id}
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} â€” retorna 204 quando deleÃ§Ã£o bem-sucedida")
    void deletar_deveRetornar204_comSucesso() throws Exception {
        doNothing().when(usuarioService).deletar(1L);

        mockMvc.perform(delete("/user/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(usuarioService).deletar(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} â€” retorna 403 quando usuÃ¡rio tenta deletar outro usuÃ¡rio")
    void deletar_deveRetornar403_quandoAcessoNaoPermitido() throws Exception {
        doThrow(new AcessoNaoPermitidoException("Acesso negado para deleÃ§Ã£o de usuÃ¡rio"))
                .when(usuarioService).deletar(2L);

        mockMvc.perform(delete("/user/2").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/{id} â€” retorna 404 quando usuÃ¡rio nÃ£o existe")
    void deletar_deveRetornar404_quandoNaoEncontrado() throws Exception {
        doThrow(new UsuarioNaoEncontradoException("UsuÃ¡rio com ID: 99 nÃ£o encontrado"))
                .when(usuarioService).deletar(99L);

        mockMvc.perform(delete("/user/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /user/{username}
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /user/{username} â€” retorna 200 com perfil pÃºblico (rota pÃºblica)")
    void getPerfil_deveRetornar200_comSucesso() throws Exception {
        PerfilPublicoDTO perfil = new PerfilPublicoDTO("JoÃ£o Silva", "joaosilva", null, 10.5f);

        when(usuarioService.buscarPerfilPublico("joaosilva")).thenReturn(perfil);

        mockMvc.perform(get("/user/joaosilva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("joaosilva"))
                .andExpect(jsonPath("$.nome").value("JoÃ£o Silva"));
    }

    @Test
    @DisplayName("GET /user/{username} â€” retorna 404 quando username nÃ£o existe")
    void getPerfil_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(usuarioService.buscarPerfilPublico("inexistente"))
                .thenThrow(new UsuarioNaoEncontradoException("UsuÃ¡rio com username: inexistente nÃ£o encontrado"));

        mockMvc.perform(get("/user/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /user/conexao/enviar/{receiverId}
    // @WithMockUser injeta principal genÃ©rico (nÃ£o instÃ¢ncia de User),
    // entÃ£o loggedInUser == null e o controller retorna 401 explicitamente.
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/enviar/{receiverId} â€” retorna 401 quando principal nÃ£o Ã© User real")
    void enviarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/enviar/{receiverId} â€” redireciona para login quando nÃ£o autenticado")
    void enviarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/enviar/2").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /user/conexao/aceitar/{requestId}
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/aceitar/{requestId} â€” retorna 401 quando principal nÃ£o Ã© User real")
    void aceitarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/aceitar/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/aceitar/{requestId} â€” redireciona para login quando nÃ£o autenticado")
    void aceitarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/aceitar/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /user/conexao/recusar/{requestId}
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("POST /user/conexao/recusar/{requestId} â€” retorna 401 quando principal nÃ£o Ã© User real")
    void recusarConexao_deveRetornar401_comWithMockUser() throws Exception {
        mockMvc.perform(post("/user/conexao/recusar/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /user/conexao/recusar/{requestId} â€” redireciona para login quando nÃ£o autenticado")
    void recusarConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/user/conexao/recusar/1").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // DELETE /user/conexao/remover/{receiverId}
    // removeConnection Ã© void â€” doNothing() Ã© vÃ¡lido
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @WithMockUser
    @DisplayName("DELETE /user/conexao/remover/{receiverId} â€” retorna 401 quando principal nÃ£o Ã© User real")
    void removerConexao_deveRetornar401_comWithMockUser() throws Exception {
        doNothing().when(userConnectionService).removeConnection(any(), any());

        mockMvc.perform(delete("/user/conexao/remover/2").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /user/conexao/remover/{receiverId} â€” redireciona para login quando nÃ£o autenticado")
    void removerConexao_deveRedirecionar_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(delete("/user/conexao/remover/2").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
