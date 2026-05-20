package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.config.security.AutenticacaoFilter;
import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.user.LoginDto;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.enums.Papel;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import br.ufpb.dsc.corrida.service.usuario.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService service;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository repository;

    @MockBean
    private AutenticacaoFilter autenticacaoFilter;

    @Test
    void deveRegistrarUsuarioComSucesso() throws Exception {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("Nome", "username", "login", "senha");
        when(service.registrar(any(RegistrarUsuarioDTO.class))).thenReturn("fakeToken");

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fakeToken"))
                .andExpect(jsonPath("$.mensagem").value("Usuário registrado com sucesso"));

        verify(service, times(1)).registrar(any(RegistrarUsuarioDTO.class));
    }

    @Test
    void deveLancarErroDeValidacaoRegistrarSemCamposObrigatorios() throws Exception {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("", "", "", "");

        mockMvc.perform(post("/user/registrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void deveFazerLoginComSucesso() throws Exception {
        LoginDto dto = new LoginDto("login", "senha");
        when(service.login(any(LoginDto.class))).thenReturn("fakeToken");

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fakeToken"))
                .andExpect(jsonPath("$.mensagem").value("Autenticado com sucesso"));

        verify(service, times(1)).login(any(LoginDto.class));
    }

    @Test
    void deveLancarErroDeValidacaoLoginSemCamposObrigatorios() throws Exception {
        LoginDto dto = new LoginDto("", "");

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).login(any());
    }

    @Test
    @WithMockUser
    void deveEditarUsuarioComSucesso() throws Exception {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Novo Nome", null, null, null);
        Usuario usuario = new Usuario(1L, "Novo Nome", "username", "login", "senha", Papel.USUARIO, false);
        when(service.editar(any(EditarUsuarioDTO.class), eq(1L))).thenReturn(usuario);

        mockMvc.perform(patch("/user/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Novo Nome"));

        verify(service, times(1)).editar(any(EditarUsuarioDTO.class), eq(1L));
    }

    @Test
    @WithMockUser
    void deveDeletarUsuarioComSucesso() throws Exception {
        doNothing().when(service).deletar(1L);

        mockMvc.perform(delete("/user/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deletar(1L);
    }
}
