package br.ufpb.dsc.corrida.home;

import br.ufpb.dsc.corrida.race.CorridaService;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.StatusCorrida;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UsuarioService;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o endpoint {@code GET /} (home page) do {@link HomeController}.
 *
 * <p>Usa MockMvc com MockBeans para isolar a camada web sem tocar o banco.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HomeController — Endpoint GET /")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CorridaService corridaService;

    @MockBean
    private UsuarioService usuarioService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Race buildRace(String slug, String nome, OffsetDateTime dataInicio) {
        Race race = new Race();
        race.setSlug(slug);
        race.setNome(nome);
        race.setDescricao("Descrição de " + nome);
        race.setDataInicio(dataInicio);
        race.setStatus(StatusCorrida.PUBLICADA);
        race.setLargadaEndereco("Av. Epitácio Pessoa, João Pessoa - PB");
        return race;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET / — retorna status 200 e view 'index'")
    void homePageDeveRetornarStatus200EViewCorreta() throws Exception {
        when(corridaService.listarProximasCorridas()).thenReturn(Collections.emptyList());
        when(usuarioService.listarUsuariosRecentes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET / — model contém atributo 'proximasCorridas'")
    void homePageDeveConterAtributoProximasCorridas() throws Exception {
        Race race = buildRace("corrida-home-test", "Corrida Home Test", OffsetDateTime.now().plusDays(10));
        when(corridaService.listarProximasCorridas()).thenReturn(List.of(race));
        when(usuarioService.listarUsuariosRecentes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("proximasCorridas"))
                .andExpect(model().attribute("proximasCorridas", hasSize(1)));
    }

    @Test
    @DisplayName("GET / — model contém atributo 'usuariosRecentes'")
    void homePageDeveConterAtributoUsuariosRecentes() throws Exception {
        PerfilPublicoDTO user = new PerfilPublicoDTO("Maria Silva", "maria_silva", "", 0.0f);
        when(corridaService.listarProximasCorridas()).thenReturn(Collections.emptyList());
        when(usuarioService.listarUsuariosRecentes()).thenReturn(List.of(user));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("usuariosRecentes"))
                .andExpect(model().attribute("usuariosRecentes", hasSize(1)));
    }

    @Test
    @DisplayName("GET / — com banco vazio: ambos os atributos são listas vazias (não deve errar)")
    void homePageComBancoVazioDeveRetornarListasVazias() throws Exception {
        when(corridaService.listarProximasCorridas()).thenReturn(Collections.emptyList());
        when(usuarioService.listarUsuariosRecentes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("proximasCorridas", empty()))
                .andExpect(model().attribute("usuariosRecentes", empty()));
    }

    @Test
    @DisplayName("GET / — retorna exatamente os dados mockados de corridas (não mais que o limite)")
    void homePageDeveRespeitar6CorridasLimit() throws Exception {
        // Simula que o service já retornou apenas 6 (limite feito na query)
        List<Race> seisRaces = List.of(
                buildRace("s1", "Race 1", OffsetDateTime.now().plusDays(1)),
                buildRace("s2", "Race 2", OffsetDateTime.now().plusDays(2)),
                buildRace("s3", "Race 3", OffsetDateTime.now().plusDays(3)),
                buildRace("s4", "Race 4", OffsetDateTime.now().plusDays(4)),
                buildRace("s5", "Race 5", OffsetDateTime.now().plusDays(5)),
                buildRace("s6", "Race 6", OffsetDateTime.now().plusDays(6))
        );

        when(corridaService.listarProximasCorridas()).thenReturn(seisRaces);
        when(usuarioService.listarUsuariosRecentes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("proximasCorridas", hasSize(6)));
    }

    @Test
    @DisplayName("GET / — retorna exatamente os dados mockados de usuários (não mais que o limite)")
    void homePageDeveRespeitar6UsuariosLimit() throws Exception {
        List<PerfilPublicoDTO> seisUsuarios = List.of(
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f),
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f),
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f),
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f),
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f),
                new PerfilPublicoDTO("User 6", "user6", "", 0.0f)
        );

        when(corridaService.listarProximasCorridas()).thenReturn(Collections.emptyList());
        when(usuarioService.listarUsuariosRecentes()).thenReturn(seisUsuarios);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("usuariosRecentes", hasSize(6)));
    }

    @Test
    @DisplayName("GET / — dados das corridas são repassados corretamente ao model")
    void homePageDeveConterDadosDasCorridas() throws Exception {
        Race race = buildRace("corrida-dados", "Corrida de João Pessoa", OffsetDateTime.now().plusDays(7));
        when(corridaService.listarProximasCorridas()).thenReturn(List.of(race));
        when(usuarioService.listarUsuariosRecentes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("proximasCorridas",
                        hasItem(hasProperty("nome", is("Corrida de João Pessoa")))));
    }

    @Test
    @DisplayName("GET / — dados dos usuários são repassados corretamente ao model")
    void homePageDeveConterDadosDosUsuarios() throws Exception {
        PerfilPublicoDTO user = new PerfilPublicoDTO("Fernanda Corrida", "fernanda_c", "", 0.0f);
        when(corridaService.listarProximasCorridas()).thenReturn(Collections.emptyList());
        when(usuarioService.listarUsuariosRecentes()).thenReturn(List.of(user));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("usuariosRecentes",
                        hasItem(hasProperty("nome", is("Fernanda Corrida")))));
    }
}
