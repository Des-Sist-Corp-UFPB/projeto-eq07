package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.ExternalServiceException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CorridaController â€” Web/API Tests")
public class CorridaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CorridaService service;

    @Test
    @DisplayName("POST /organizacao/{id}/corridas â€” should return 200 and form view with errorMessage when service throws ExternalServiceException")
    void postCorridaExternalServiceExceptionReturnsForm() throws Exception {

        User usuarioMock = new User();
        usuarioMock.setUsername("organizador_teste");
        usuarioMock.setPapel(Papel.ORGANIZADOR);
        usuarioMock.setLogin("wil@gmail.com");
        usuarioMock.setNome("Peterson William");

        when(service.criarCorrida(any(CriarCorridaDTO.class), eq(100L), any()))
                .thenThrow(new ExternalServiceException("NÃ£o foi possÃ­vel calcular a rota neste momento"));

        mockMvc.perform(post("/organizacao/100/corridas")
                        .with(csrf())
                        .with(user(usuarioMock))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Corrida Teste")
                        .param("descricao", "DescriÃ§Ã£o")
                        .param("dataInicio", OffsetDateTime.now().plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
                        .param("categoria", "C5K")
                        .param("largadaLat", "-7.115")
                        .param("largadaLng", "-34.863")
                        .param("largadaEndereco", "Largada")
                        .param("chegadaLat", "-7.115")
                        .param("chegadaLng", "-34.863")
                        .param("chegadaEndereco", "Chegada"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corrida-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZADOR")
    @DisplayName("POST /organizacao/{id}/corridas â€” should return 403 when user is not the owner of organization")
    void postCorridaNonOwnerReturns403() throws Exception {

        User usuarioMock = new User();
        usuarioMock.setUsername("organizador_teste");
        usuarioMock.setPapel(Papel.ORGANIZADOR);
        usuarioMock.setLogin("wil@gmail.com");
        usuarioMock.setNome("Peterson William");

        when(service.criarCorrida(any(CriarCorridaDTO.class), eq(100L), any()))
                .thenThrow(new AcessoNaoPermitidoException("Acesso negado"));

        mockMvc.perform(post("/organizacao/100/corridas")
                        .with(csrf())
                        .with(user(usuarioMock))
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Corrida Teste")
                        .param("descricao", "DescriÃ§Ã£o")
                        .param("dataInicio", OffsetDateTime.now().plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
                        .param("categoria", "C5K")
                        .param("largadaLat", "-7.115")
                        .param("largadaLng", "-34.863")
                        .param("largadaEndereco", "Largada")
                        .param("chegadaLat", "-7.115")
                        .param("chegadaLng", "-34.863")
                        .param("chegadaEndereco", "Chegada"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORGANIZADOR")
    @DisplayName("GET /organizacao/{id}/corridas/{raceId}/editar â€” should return 403 when user is not the owner of organization")
    void getEditFormNonOwnerReturns403() throws Exception {
        when(service.buscarPorId(500L)).thenReturn(new Race());
        // In the controller, we call service.editarCorrida or service.buscarPorId and do checking.
        // Let's assume we do a search or check in view controller that throws AcessoNaoPermitidoException
        // when owner check fails.
        when(service.buscarPorId(500L)).thenThrow(new AcessoNaoPermitidoException("Acesso negado"));

        mockMvc.perform(get("/organizacao/100/corridas/500/editar"))
                .andExpect(status().isForbidden());
    }
}

