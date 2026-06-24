package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("CorridaViewController — Web View Tests")
public class CorridaViewControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CorridaService service;

    @MockBean
    private OrganizerService organizerService;

    private User ownerUser;
    private User nonOwnerUser;
    private Organizer ownerOrganizer;
    private Organizer nonOwnerOrganizer;
    private Organization organization;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setUsername("dono_org");
        ownerUser.setPapel(Papel.ORGANIZADOR);
        ownerUser.setLogin("dono@gmail.com");

        nonOwnerUser = new User();
        nonOwnerUser.setId(2L);
        nonOwnerUser.setUsername("nao_dono");
        nonOwnerUser.setPapel(Papel.ORGANIZADOR);
        nonOwnerUser.setLogin("naodono@gmail.com");

        ownerOrganizer = new Organizer();
        ownerOrganizer.setId(10L);
        ownerOrganizer.setUsuario(ownerUser);

        nonOwnerOrganizer = new Organizer();
        nonOwnerOrganizer.setId(20L);
        nonOwnerOrganizer.setUsuario(nonOwnerUser);

        organization = new Organization();
        organization.setId(100L);
        organization.setName("Org Teste");
        organization.setOrganizer(ownerOrganizer);
    }

    // =========================================================================
    // Feed Público
    // =========================================================================

    @Test
    @DisplayName("GET /corridas — should return public future races page successfully")
    void getCorridasPublicasSuccess() throws Exception {
        Race race = new Race();
        race.setNome("Maratona Teste");
        when(service.listarCorridasPublicas()).thenReturn(List.of(race));

        mockMvc.perform(get("/corridas"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corridas-lista"))
                .andExpect(model().attribute("titulo", "Próximas Corridas"))
                .andExpect(model().attribute("isHistorico", false))
                .andExpect(model().attribute("activePage", "corridas"))
                .andExpect(model().attributeExists("corridas"));
    }

    @Test
    @DisplayName("GET /corridas/encerradas — should return public history races page successfully")
    void getCorridasHistoricoSuccess() throws Exception {
        Race race = new Race();
        race.setNome("Meia Maratona Antiga");
        when(service.listarHistorico()).thenReturn(List.of(race));

        mockMvc.perform(get("/corridas/encerradas"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corridas-lista"))
                .andExpect(model().attribute("titulo", "Histórico de Corridas"))
                .andExpect(model().attribute("isHistorico", true))
                .andExpect(model().attribute("activePage", "corridas"))
                .andExpect(model().attributeExists("corridas"));
    }

    @Test
    @DisplayName("GET /corridas/{slug} — should return race details page successfully")
    void getCorridaDetalhesSuccess() throws Exception {
        Race race = new Race();
        race.setNome("Corrida de Exemplo");
        race.setSlug("corrida-de-exemplo");
        
        Organization org = new Organization();
        org.setId(1L); 
        race.setOrganization(org);
        when(service.buscarPorSlug("corrida-de-exemplo")).thenReturn(race);

        mockMvc.perform(get("/corridas/corrida-de-exemplo"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corrida-detalhes"))
                .andExpect(model().attribute("corrida", race))
                .andExpect(model().attribute("activePage", "corridas"));
    }

    @Test
    @DisplayName("GET /corridas/{slug} — should return 404 when race is not found")
    void getCorridaDetalhesNotFound() throws Exception {
        when(service.buscarPorSlug("invalido")).thenThrow(new CorridaNaoEncontradaException("Corrida não encontrada"));

        mockMvc.perform(get("/corridas/invalido"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Gestão do Organizador
    // =========================================================================

    @Test
    @DisplayName("GET /organizacao/{orgId}/corridas — should return management list when owner")
    void getGerenciarCorridasOwner() throws Exception {
        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(organization);
        when(organizerService.buscarOrganizadorPorUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(organizerService.buscarOrganizacaoPorOrganizadorId(10L)).thenReturn(Optional.of(organization));
        when(service.listarPorOrganizacao(100L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/organizacao/100/corridas")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corridas-gerenciar"))
                .andExpect(model().attribute("orgId", 100L))
                .andExpect(model().attribute("activePage", "minhas-corridas"));
    }

    @Test
    @DisplayName("GET /organizacao/{orgId}/corridas — should return 403 when non-owner")
    void getGerenciarCorridasNonOwner() throws Exception {
        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(organization);
        when(organizerService.buscarOrganizadorPorUsuarioId(2L)).thenReturn(Optional.of(nonOwnerOrganizer));
        when(organizerService.buscarOrganizacaoPorOrganizadorId(20L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/organizacao/100/corridas")
                        .with(user(nonOwnerUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /organizacao/{orgId}/corridas/nova — should return creation form when owner")
    void getNovaCorridaFormOwner() throws Exception {
        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(organization);
        when(organizerService.buscarOrganizadorPorUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(organizerService.buscarOrganizacaoPorOrganizadorId(10L)).thenReturn(Optional.of(organization));

        mockMvc.perform(get("/organizacao/100/corridas/nova")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corrida-form"))
                .andExpect(model().attribute("orgId", 100L))
                .andExpect(model().attribute("isEdicao", false))
                .andExpect(model().attributeExists("categorias"))
                .andExpect(model().attributeExists("beneficios"))
                .andExpect(model().attributeExists("corrida"));
    }

    @Test
    @DisplayName("GET /organizacao/{orgId}/corridas/{id}/editar — should return edit form when owner")
    void getEditarCorridaFormOwner() throws Exception {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);

        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(organization);
        when(organizerService.buscarOrganizadorPorUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(organizerService.buscarOrganizacaoPorOrganizadorId(10L)).thenReturn(Optional.of(organization));
        when(service.buscarPorId(500L)).thenReturn(race);

        mockMvc.perform(get("/organizacao/100/corridas/500/editar")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/corrida-form"))
                .andExpect(model().attribute("orgId", 100L))
                .andExpect(model().attribute("raceId", 500L))
                .andExpect(model().attribute("isEdicao", true))
                .andExpect(model().attributeExists("categorias"))
                .andExpect(model().attributeExists("beneficios"))
                .andExpect(model().attributeExists("corrida"));
    }

    @Test
    @DisplayName("GET /organizacao/{orgId}/corridas/{id}/editar — should return 403 when race does not belong to organization")
    void getEditarCorridaFormWrongOrg() throws Exception {
        Organization otherOrg = new Organization();
        otherOrg.setId(200L); // different org ID

        Race race = new Race();
        race.setId(500L);
        race.setOrganization(otherOrg); // race belongs to other org

        when(organizerService.buscarOrganizacaoPorId(100L)).thenReturn(organization);
        when(organizerService.buscarOrganizadorPorUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(organizerService.buscarOrganizacaoPorOrganizadorId(10L)).thenReturn(Optional.of(organization));
        when(service.buscarPorId(500L)).thenReturn(race);

        mockMvc.perform(get("/organizacao/100/corridas/500/editar")
                        .with(user(ownerUser)))
                .andExpect(status().isForbidden());
    }
}
