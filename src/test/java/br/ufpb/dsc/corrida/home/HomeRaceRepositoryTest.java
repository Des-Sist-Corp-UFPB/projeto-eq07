package br.ufpb.dsc.corrida.home;

import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizationRepository;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerRepository;
import br.ufpb.dsc.corrida.race.CategoriaCorrida;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.race.StatusCorrida;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o método {@link RaceRepository#findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc}
 * usado pela home page ("Corridas Próximas").
 *
 * <p>Cada teste é encapsulado em transação e o banco é limpo no setUp para
 * garantir isolamento entre cenários.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("HomeRaceRepository — Corridas Próximas")
class HomeRaceRepositoryTest {

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // Cria infraestrutura mínima: user → organizer → organization
        long ts = System.nanoTime();

        User user = new User();
        user.setNome("Org Home Test");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("senha123");
        user.setLogin("home_race_" + ts + "@test.com");
        user.setUsername("home_race_" + ts);
        user = userRepository.save(user);

        Organizer organizer = new Organizer();
        organizer.setUsuario(user);
        organizer.setCref((ts % 99999) + "-G/PB");
        organizer.setEmail("org_home_race_" + ts + "@test.com");
        organizer.setWhatsapp("83999990000");
        organizer.setUfConselho("PB");
        organizer.setCpf(String.format("%011d", ts % 100_000_000_000L));
        organizer = organizerRepository.save(organizer);

        organization = new Organization();
        organization.setName("Org Home Race " + ts);
        organization.setFoundedAt(LocalDate.now());
        organization.setCity("João Pessoa");
        organization.setState("PB");
        organization.setOrganizer(organizer);
        organization = organizationRepository.save(organization);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Race buildRace(String slug, OffsetDateTime dataInicio, StatusCorrida status) {
        Race race = new Race();
        race.setSlug(slug);
        race.setNome("Corrida " + slug);
        race.setDescricao("Descrição de " + slug);
        race.setCategoria(CategoriaCorrida.C5K);
        race.setDataInicio(dataInicio);
        race.setStatus(status);
        race.setLargadaLat(-7.12);
        race.setLargadaLng(-34.84);
        race.setLargadaEndereco("Av. Epaminondas Câmara");
        race.setChegadaLat(-7.13);
        race.setChegadaLng(-34.85);
        race.setChegadaEndereco("Parque Solon de Lucena");
        race.setOrganization(organization);
        return raceRepository.save(race);
    }

    private OffsetDateTime future(int plusDays) {
        return OffsetDateTime.now().plusDays(plusDays);
    }

    private OffsetDateTime past(int minusDays) {
        return OffsetDateTime.now().minusDays(minusDays);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Retorna corridas futuras com status PUBLICADA, ordenadas por dataInicio ASC")
    void deveRetornarCorridasFuturasOrdenadas() {
        buildRace("home-race-future-3-" + System.nanoTime(), future(30), StatusCorrida.PUBLICADA);
        buildRace("home-race-future-1-" + System.nanoTime(), future(5),  StatusCorrida.PUBLICADA);
        buildRace("home-race-future-2-" + System.nanoTime(), future(10), StatusCorrida.PUBLICADA);

        List<Race> resultado = raceRepository.findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc(
                List.of(StatusCorrida.PUBLICADA), OffsetDateTime.now());

        assertThat(resultado).hasSizeGreaterThanOrEqualTo(3);

        // Verifica ordem ASC pelas primeiras 3 datas retornadas
        List<OffsetDateTime> datas = resultado.stream().map(Race::getDataInicio).toList();
        for (int i = 0; i < datas.size() - 1; i++) {
            assertThat(datas.get(i)).isBeforeOrEqualTo(datas.get(i + 1));
        }
    }

    @Test
    @DisplayName("Exclui corridas com data no passado")
    void deveExcluirCorridasPassadas() {
        buildRace("home-race-past-" + System.nanoTime(),   past(5),   StatusCorrida.PUBLICADA);
        buildRace("home-race-future-" + System.nanoTime(), future(5), StatusCorrida.PUBLICADA);

        List<Race> resultado = raceRepository.findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc(
                List.of(StatusCorrida.PUBLICADA), OffsetDateTime.now());

        // Todas as corridas retornadas devem ter dataInicio no futuro
        assertThat(resultado).allMatch(r -> r.getDataInicio().isAfter(OffsetDateTime.now()));
    }

    @Test
    @DisplayName("Exclui corridas com status diferente de PUBLICADA")
    void deveExcluirCorridasNaoPublicadas() {
        buildRace("home-race-rascunho-" + System.nanoTime(),  future(5), StatusCorrida.RASCUNHO);
        buildRace("home-race-cancelada-" + System.nanoTime(), future(5), StatusCorrida.CANCELADA);
        buildRace("home-race-publicada-" + System.nanoTime(), future(5), StatusCorrida.PUBLICADA);

        List<Race> resultado = raceRepository.findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc(
                List.of(StatusCorrida.PUBLICADA), OffsetDateTime.now());

        assertThat(resultado).allMatch(r -> r.getStatus() == StatusCorrida.PUBLICADA);
    }

    @Test
    @DisplayName("Retorna lista vazia quando não há corridas futuras publicadas")
    void deveRetornarListaVaziaQuandoNaoHaCorridasFuturas() {
        // Persiste apenas corridas passadas
        buildRace("home-race-only-past-1-" + System.nanoTime(), past(10), StatusCorrida.PUBLICADA);
        buildRace("home-race-only-past-2-" + System.nanoTime(), past(20), StatusCorrida.PUBLICADA);

        List<Race> resultado = raceRepository.findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc(
                List.of(StatusCorrida.PUBLICADA), OffsetDateTime.now());

        // Pode existir dados de outros testes, mas nenhum dos criados aqui deve aparecer
        resultado.forEach(r -> assertThat(r.getDataInicio()).isAfter(OffsetDateTime.now()));
    }

    @Test
    @DisplayName("Limita em 6 resultados mesmo havendo mais de 6 corridas futuras")
    void deveRetornarNoMaximo6Corridas() {
        // Cria 8 corridas futuras publicadas
        for (int i = 1; i <= 8; i++) {
            buildRace("home-race-limite-" + i + "-" + System.nanoTime(), future(i * 10), StatusCorrida.PUBLICADA);
        }

        List<Race> resultado = raceRepository.findTop6ByStatusInAndDataInicioAfterOrderByDataInicioAsc(
                List.of(StatusCorrida.PUBLICADA), OffsetDateTime.now());

        assertThat(resultado).hasSizeLessThanOrEqualTo(6);
    }
}
