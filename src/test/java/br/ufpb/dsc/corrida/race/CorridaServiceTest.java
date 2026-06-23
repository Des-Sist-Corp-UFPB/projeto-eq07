package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizationRepository;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerRepository;
import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.EditarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.RotaDTO;
import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorridaService — Unit Tests")
public class CorridaServiceTest {

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizerRepository organizerRepository;

    @Mock
    private OpenRouteServiceClient orsClient;

    @InjectMocks
    private CorridaService service;

    private User ownerUser;
    private User nonOwnerUser;
    private Organizer ownerOrganizer;
    private Organizer nonOwnerOrganizer;
    private Organization organization;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(1L);

        nonOwnerUser = new User();
        nonOwnerUser.setId(2L);

        ownerOrganizer = new Organizer();
        ownerOrganizer.setId(10L);
        ownerOrganizer.setUsuario(ownerUser);

        nonOwnerOrganizer = new Organizer();
        nonOwnerOrganizer.setId(20L);
        nonOwnerOrganizer.setUsuario(nonOwnerUser);

        organization = new Organization();
        organization.setId(100L);
        organization.setOrganizer(ownerOrganizer);
    }

    // =========================================================================
    // Criação
    // =========================================================================

    @Test
    @DisplayName("criarCorrida — success")
    void criarCorridaSuccess() {
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(organization));
        when(organizerRepository.findByUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(raceRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(orsClient.calcularRota(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RotaDTO("{geojson}", BigDecimal.valueOf(10.0), 60));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CriarCorridaDTO dto = new CriarCorridaDTO(
                "Super Corrida", "Desc", null, BigDecimal.TEN, 100,
                OffsetDateTime.now().plusDays(5), CategoriaCorrida.C5K,
                -7.115, -34.863, "Start", -7.125, -34.873, "End", Collections.emptySet()
        );

        Race race = service.criarCorrida(dto, 100L, ownerUser);

        assertThat(race).isNotNull();
        assertThat(race.getNome()).isEqualTo("Super Corrida");
        assertThat(race.getStatus()).isEqualTo(StatusCorrida.RASCUNHO);
        assertThat(race.getSlug()).startsWith("super-corrida");
        assertThat(race.getDistanciaKm()).isEqualTo(BigDecimal.valueOf(10.0));
        assertThat(race.getDuracaoEstimadaMin()).isEqualTo(60);
        verify(orsClient, times(1)).calcularRota(-34.863, -7.115, -34.873, -7.125);
    }

    @Test
    @DisplayName("criarCorrida — not owner throws AcessoNaoPermitidoException")
    void criarCorridaNotOwnerThrowsException() {
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(organization));
        when(organizerRepository.findByUsuarioId(2L)).thenReturn(Optional.of(nonOwnerOrganizer));

        CriarCorridaDTO dto = new CriarCorridaDTO(
                "Maratona", "Desc", null, BigDecimal.ZERO, 10,
                OffsetDateTime.now().plusDays(5), CategoriaCorrida.C5K,
                0.0, 0.0, "Start", 1.0, 1.0, "End", Collections.emptySet()
        );

        assertThatThrownBy(() -> service.criarCorrida(dto, 100L, nonOwnerUser))
                .isInstanceOf(AcessoNaoPermitidoException.class);
    }

    // =========================================================================
    // Edição
    // =========================================================================

    @Test
    @DisplayName("editarCorrida — success with coordinates change (calls ORS)")
    void editarCorridaDiffCoordsCallsORS() {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);
        race.setDataInicio(OffsetDateTime.now().plusDays(2));
        race.setLargadaLat(-7.11);
        race.setLargadaLng(-34.86);
        race.setChegadaLat(-7.12);
        race.setChegadaLng(-34.87);

        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));
        when(organizerRepository.findByUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(orsClient.calcularRota(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RotaDTO("{new_geojson}", BigDecimal.valueOf(5.2), 35));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EditarCorridaDTO dto = new EditarCorridaDTO(
                "Maratona Modificada", "Desc", null, BigDecimal.ZERO, 10,
                OffsetDateTime.now().plusDays(2), CategoriaCorrida.C5K,
                -7.11, -34.86, "Start", -8.00, -35.00, "End", Collections.emptySet() // different llegada
        );

        Race updated = service.editarCorrida(500L, dto, ownerUser);

        assertThat(updated.getDistanciaKm()).isEqualTo(BigDecimal.valueOf(5.2));
        assertThat(updated.getDuracaoEstimadaMin()).isEqualTo(35);
        assertThat(updated.getRotaGeoJson()).isEqualTo("{new_geojson}");
        verify(orsClient, times(1)).calcularRota(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("editarCorrida — success with same coordinates (skips ORS)")
    void editarCorridaSameCoordsSkipsORS() {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);
        race.setDataInicio(OffsetDateTime.now().plusDays(2));
        race.setLargadaLat(-7.11);
        race.setLargadaLng(-34.86);
        race.setChegadaLat(-7.12);
        race.setChegadaLng(-34.87);
        race.setDistanciaKm(BigDecimal.TEN);
        race.setDuracaoEstimadaMin(60);
        race.setRotaGeoJson("{geojson}");

        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));
        when(organizerRepository.findByUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EditarCorridaDTO dto = new EditarCorridaDTO(
                "Maratona Modificada", "Desc", null, BigDecimal.ZERO, 10,
                OffsetDateTime.now().plusDays(2), CategoriaCorrida.C5K,
                -7.11, -34.86, "Start", -7.12, -34.87, "End", Collections.emptySet()
        );

        Race updated = service.editarCorrida(500L, dto, ownerUser);

        assertThat(updated.getNome()).isEqualTo("Maratona Modificada");
        assertThat(updated.getDistanciaKm()).isEqualTo(BigDecimal.TEN);
        verifyNoInteractions(orsClient);
    }

    @Test
    @DisplayName("editarCorrida — not owner throws AcessoNaoPermitidoException")
    void editarCorridaNotOwnerThrowsException() {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);
        race.setDataInicio(OffsetDateTime.now().plusDays(2));

        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));
        when(organizerRepository.findByUsuarioId(2L)).thenReturn(Optional.of(nonOwnerOrganizer));

        EditarCorridaDTO dto = new EditarCorridaDTO(
                "Maratona", "Desc", null, BigDecimal.ZERO, 10,
                OffsetDateTime.now().plusDays(2), CategoriaCorrida.C5K,
                0.0, 0.0, "Start", 1.0, 1.0, "End", Collections.emptySet()
        );

        assertThatThrownBy(() -> service.editarCorrida(500L, dto, nonOwnerUser))
                .isInstanceOf(AcessoNaoPermitidoException.class);
    }

    @Test
    @DisplayName("editarCorrida — throws IllegalStateException when starts in less than 24h")
    void editarCorridaTooLateThrowsException() {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);
        race.setDataInicio(OffsetDateTime.now().plusHours(12)); // < 24h

        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));
        when(organizerRepository.findByUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));

        EditarCorridaDTO dto = new EditarCorridaDTO(
                "Maratona", "Desc", null, BigDecimal.ZERO, 10,
                OffsetDateTime.now().plusHours(12), CategoriaCorrida.C5K,
                0.0, 0.0, "Start", 1.0, 1.0, "End", Collections.emptySet()
        );

        assertThatThrownBy(() -> service.editarCorrida(500L, dto, ownerUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Corridas só podem ser editadas com mais de 24 horas de antecedência");
    }

    // =========================================================================
    // Cancelamento
    // =========================================================================

    @Test
    @DisplayName("cancelarCorrida — success")
    void cancelarCorridaSuccess() {
        Race race = new Race();
        race.setId(500L);
        race.setOrganization(organization);
        race.setStatus(StatusCorrida.PUBLICADA);

        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));
        when(organizerRepository.findByUsuarioId(1L)).thenReturn(Optional.of(ownerOrganizer));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarCorrida(500L, ownerUser);

        assertThat(race.getStatus()).isEqualTo(StatusCorrida.CANCELADA);
        verify(raceRepository, times(1)).save(race);
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    @Test
    @DisplayName("listarCorridas — returns only future PUBLICADA races")
    void listarCorridasFiltersMix() {
        Race futurePublic = new Race();
        futurePublic.setStatus(StatusCorrida.PUBLICADA);
        futurePublic.setDataInicio(OffsetDateTime.now().plusDays(2));

        Race pastPublic = new Race();
        pastPublic.setStatus(StatusCorrida.PUBLICADA);
        pastPublic.setDataInicio(OffsetDateTime.now().minusDays(1));

        Page<Race> page = new PageImpl<>(List.of(futurePublic));

        when(raceRepository.findAllByStatusInAndDataInicioAfter(any(), any() ,any(Pageable.class)))
                .thenReturn(page);

        Page<Race> result = service.listarCorridas(PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(futurePublic);
    }

    @Test
    @DisplayName("listarCorridasPublicas — returns only future PUBLICADA races")
    void listarCorridasPublicasFiltersMix() {
        Race futurePublic = new Race();
        futurePublic.setStatus(StatusCorrida.PUBLICADA);
        futurePublic.setDataInicio(OffsetDateTime.now().plusDays(2));

        Race pastPublic = new Race();
        pastPublic.setStatus(StatusCorrida.PUBLICADA);
        pastPublic.setDataInicio(OffsetDateTime.now().minusDays(1));

        Page<Race> page = new PageImpl<>(List.of(futurePublic));

        when(raceRepository.findAllByStatusInAndDataInicioAfter(any(), any(),any(Pageable.class)))
                .thenReturn(page);

        List<Race> result = service.listarCorridasPublicas();

        assertThat(result).containsExactly(futurePublic);
    }

    @Test
    @DisplayName("listarHistorico — returns only ENCERRADA races")
    void listarHistoricoReturnsOnlyEncerrada() {
        Race encerrada = new Race();
        encerrada.setStatus(StatusCorrida.ENCERRADA);

        when(raceRepository.findAllByStatus(StatusCorrida.ENCERRADA)).thenReturn(List.of(encerrada));

        List<Race> result = service.listarHistorico();

        assertThat(result).containsExactly(encerrada);
    }

    @Test
    @DisplayName("listarPorOrganizacao — success")
    void listarPorOrganizacaoSuccess() {
        Race race1 = new Race();
        Race race2 = new Race();
        when(raceRepository.findAllByOrganizationId(100L)).thenReturn(List.of(race1, race2));

        List<Race> result = service.listarPorOrganizacao(100L);

        assertThat(result).hasSize(2);
        verify(raceRepository, times(1)).findAllByOrganizationId(100L);
    }

    @Test
    @DisplayName("buscarPorSlug — success when status is PUBLICADA")
    void buscarPorSlugSuccess() {
        Race race = new Race();
        race.setStatus(StatusCorrida.PUBLICADA);
        when(raceRepository.findBySlug("corrida-legal")).thenReturn(Optional.of(race));

        Race result = service.buscarPorSlug("corrida-legal");

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(race);
    }

    @Test
    @DisplayName("buscarPorSlug — throws CorridaNaoEncontradaException when status is CANCELADA")
    void buscarPorSlugCancelledThrowsException() {
        Race race = new Race();
        race.setStatus(StatusCorrida.CANCELADA);
        when(raceRepository.findBySlug("corrida-cancelada")).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> service.buscarPorSlug("corrida-cancelada"))
                .isInstanceOf(CorridaNaoEncontradaException.class)
                .hasMessageContaining("Corrida não encontrada");
    }

    @Test
    @DisplayName("buscarPorId — success")
    void buscarPorIdSuccess() {
        Race race = new Race();
        when(raceRepository.findById(500L)).thenReturn(Optional.of(race));

        Race result = service.buscarPorId(500L);

        assertThat(result).isEqualTo(race);
    }

    @Test
    @DisplayName("buscarPorId — throws CorridaNaoEncontradaException when not found")
    void buscarPorIdNotFound() {
        when(raceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(999L))
                .isInstanceOf(CorridaNaoEncontradaException.class);
    }
}
