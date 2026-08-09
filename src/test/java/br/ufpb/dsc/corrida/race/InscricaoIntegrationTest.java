package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.InscricaoService;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizationRepository;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerRepository;
import br.ufpb.dsc.corrida.user.Genero;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InscricaoIntegrationTest {

    @Autowired
    private InscricaoService inscricaoService;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private User atleta;
    private User organizadorUser;
    private Race corrida;

    @BeforeEach
    void setUp() {
        atleta = new User();
        atleta.setNome("Atleta Teste");
        atleta.setLogin("atleta@teste.com");
        atleta.setUsername("atleta");
        atleta.setSenha("123");
        atleta.setPapel(Papel.USUARIO);
        atleta.setDeletado(false);
        userRepository.save(atleta);

        UserInfo userInfo = new UserInfo();
        userInfo.setUsuario(atleta);
        userInfo.setPeso(70f);
        userInfo.setAltura(170f);
        userInfo.setGenero(Genero.MALE);
        userInfo.setDataNasc(LocalDate.of(1990, 1, 1));
        userInfo.setTotalKmRun(0f);
        userInfoRepository.save(userInfo);

        organizadorUser = new User();
        organizadorUser.setNome("Organizador Teste");
        organizadorUser.setLogin("org@teste.com");
        organizadorUser.setUsername("organizador");
        organizadorUser.setSenha("123");
        organizadorUser.setPapel(Papel.ORGANIZADOR);
        organizadorUser.setDeletado(false);
        userRepository.save(organizadorUser);

        Organizer organizer = new Organizer();
        organizer.setUsuario(organizadorUser);
        organizer.setCpf("12345678901");
        organizer.setCref("1234-G/PB");
        organizer.setEmail("org@test.com");
        organizer.setWhatsapp("83999999999");
        organizer.setUfConselho("PB");
        organizerRepository.save(organizer);

        Organization org = new Organization();
        org.setName("Org Teste");
        org.setFoundedAt(LocalDate.now());
        org.setCity("City");
        org.setState("State");
        org.setOrganizer(organizer);
        organizationRepository.save(org);

        corrida = new Race();
        corrida.setSlug("corrida-teste");
        corrida.setNome("Corrida Teste");
        corrida.setDescricao("Descricao");
        corrida.setDataInicio(OffsetDateTime.now().plusDays(5));
        corrida.setDuracaoEstimadaMin(60);
        corrida.setStatus(StatusCorrida.PUBLICADA);
        corrida.setCategoria(CategoriaCorrida.C5K);
        corrida.setLargadaLat(0.0);
        corrida.setLargadaLng(0.0);
        corrida.setLargadaEndereco("A");
        corrida.setChegadaLat(0.0);
        corrida.setChegadaLng(0.0);
        corrida.setChegadaEndereco("B");
        corrida.setOrganization(org);
        corrida.setDistanciaKm(BigDecimal.valueOf(5.0));
        raceRepository.save(corrida);
    }

    /*@Test
    @DisplayName("Fluxo completo de Inscricao e Encerramento com ganho de KM")
    void testFluxoCompletoInscricaoEncerramento() {
        // 1. Atleta se inscreve
        Inscricao inscricao = inscricaoService.inscrever(atleta, corrida.getId(), false);
        assertNotNull(inscricao.getId());

        // 2. Verifica banco - alterado de ATIVA para CONFIRMADA
        List<Inscricao> confirmadas = inscricaoRepository.findByUsuarioAndStatus(atleta, StatusInscricao.CONFIRMADA);
        assertEquals(1, confirmadas.size());

        // 3. Checkin do organizador
        inscricaoService.marcarPresenca(organizadorUser, inscricao.getId(), true);
        Inscricao atualizada = inscricaoRepository.findById(inscricao.getId()).get();
        assertTrue(atualizada.isCompareceu());

        // 4. Encerrar corrida
        inscricaoService.processarEncerramentoCorrida(corrida);

        // 5. Verifica os Kms do atleta
        UserInfo infoAtualizada = userInfoRepository.findByUsuarioId(atleta.getId()).get();
        assertEquals(5.0f, infoAtualizada.getTotalKmRun());
    }*/
}
