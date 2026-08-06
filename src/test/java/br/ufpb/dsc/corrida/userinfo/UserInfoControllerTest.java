package br.ufpb.dsc.corrida.userinfo;

import br.ufpb.dsc.corrida.user.Genero;
import br.ufpb.dsc.corrida.user.NivelCondicionamento;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoJaExistenteException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.user.UserInfoService;
import br.ufpb.dsc.corrida.user.UsuarioService;
import br.ufpb.dsc.corrida.user.dto.AtualizarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.CriarUserInfoDTO;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserInfoController — Unit Tests")
class UserInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private UsuarioService usuarioService;

    private ObjectMapper objectMapper;
    private UserInfoRespostaDTO respostaDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        respostaDTO = new UserInfoRespostaDTO(
                1L, 1L, 70.5f, 175.0f,
                Genero.MALE, 0.0f,
                LocalDate.of(1995, 5, 20),
                null, NivelCondicionamento.INTERMEDIATE, null,
                true, null, null, null
        );
    }

    // ─────────────────────────────────────────────
    // GET /user-info/{usuarioId}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /user-info/{usuarioId} — delega para service.buscarPorUsuarioId() e retorna 200")
    void getByUserId_deveRetornar200_comSucesso() throws Exception {
        when(userInfoService.buscarPorUsuarioId(1L)).thenReturn(respostaDTO);

        mockMvc.perform(get("/user/userInfo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peso").value(70.5))
                .andExpect(jsonPath("$.altura").value(175.0));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /user-info/{usuarioId} — propaga 404 quando service lança UserInfoNaoEncontradoException")
    void getByUserId_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(userInfoService.buscarPorUsuarioId(99L))
                .thenThrow(new UserInfoNaoEncontradoException("Não encontrado"));

        mockMvc.perform(get("/user/userInfo/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────
    // POST /user-info
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /user-info — delega para service.criar() e retorna 201")
    void post_deveRetornar201_comSucesso() throws Exception {
        CriarUserInfoDTO dto = new CriarUserInfoDTO(
                1L, 70.5f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, NivelCondicionamento.INTERMEDIATE, null, null, true
        );

        when(userInfoService.criar(any(CriarUserInfoDTO.class))).thenReturn(respostaDTO);

        mockMvc.perform(post("/user/userInfo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peso").value(70.5));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user-info — propaga 409 quando service lança UserInfoJaExistenteException")
    void post_deveRetornar409_quandoJaExiste() throws Exception {
        CriarUserInfoDTO dto = new CriarUserInfoDTO(
                1L, 70.5f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, NivelCondicionamento.INTERMEDIATE, null, null, true
        );

        when(userInfoService.criar(any(CriarUserInfoDTO.class)))
                .thenThrow(new UserInfoJaExistenteException("Já existe"));

        mockMvc.perform(post("/user/userInfo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /user-info — propaga 404 quando service lança UsuarioNaoEncontradoException")
    void post_deveRetornar404_quandoUsuarioNaoExiste() throws Exception {
        CriarUserInfoDTO dto = new CriarUserInfoDTO(
                999L, 70.5f, 175.0f, Genero.MALE,
                LocalDate.of(1995, 5, 20), null, NivelCondicionamento.INTERMEDIATE, null, null, true
        );

        when(userInfoService.criar(any(CriarUserInfoDTO.class)))
                .thenThrow(new UsuarioNaoEncontradoException("Usuário não encontrado"));

        mockMvc.perform(post("/user/userInfo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────
    // PUT /user-info/{usuarioId}
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("PUT /user-info/{usuarioId} — delega para service.atualizar() e retorna 200")
    void put_deveRetornar200_comSucesso() throws Exception {
        AtualizarUserInfoDTO dto = new AtualizarUserInfoDTO(
                85.0f, null, null, null, null, null, null, true, null
        );

        UserInfoRespostaDTO atualizado = new UserInfoRespostaDTO(
                1L, 1L, 85.0f, 175.0f, Genero.MALE, 0.0f,
                LocalDate.of(1995, 5, 20), null, NivelCondicionamento.INTERMEDIATE, null,
                true, null, null, null
        );

        when(userInfoService.atualizar(eq(1L), any(AtualizarUserInfoDTO.class))).thenReturn(atualizado);

        mockMvc.perform(put("/user/userInfo/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peso").value(85.0));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /user-info/{usuarioId} — propaga 404 quando service lança UserInfoNaoEncontradoException")
    void put_deveRetornar404_quandoNaoEncontrado() throws Exception {
        AtualizarUserInfoDTO dto = new AtualizarUserInfoDTO(
                85.0f, null, null, null, null, null, null, true, null
        );

        when(userInfoService.atualizar(eq(99L), any(AtualizarUserInfoDTO.class)))
                .thenThrow(new UserInfoNaoEncontradoException("Não encontrado"));

        mockMvc.perform(put("/user/userInfo/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────
    // UPLOAD FOTO DE PERFIL
    // ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /user-info/{usuarioId}/foto-perfil — delega para service e retorna 200")
    void uploadFoto_deveRetornar200_comSucesso() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "teste.png", MediaType.IMAGE_PNG_VALUE, "imagem".getBytes()
        );

        UserInfoRespostaDTO atualizado = new UserInfoRespostaDTO(
                1L, 1L, 70.0f, 175.0f, Genero.MALE, 0.0f,
                LocalDate.of(1995, 5, 20), "/uploads/perfil/teste.png", 
                NivelCondicionamento.INTERMEDIATE, null, true, null, null, null
        );

        when(userInfoService.uploadFotoPerfil(eq(1L), any())).thenReturn(atualizado);

        mockMvc.perform(multipart("/user/userInfo/1/foto-perfil")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoPerfil").value("/uploads/perfil/teste.png"));
    }
}
