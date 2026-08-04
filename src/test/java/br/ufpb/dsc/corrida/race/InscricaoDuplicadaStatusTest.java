package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.StatusInscricao;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InscricaoDuplicadaStatusTest {

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizationRepository organizationRepository;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizerRepository organizerRepository;

    private Race criarCorridaValida(String nome, String slug, BigDecimal valor) {
        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        // IMPORTANTE: Deixar username, login e email dinâmicos para não estourar a chave única (UNIQUE)
        user.setLogin("user_" + timestamp + "@gmail.com");
        user.setUsername("atleta_" + System.currentTimeMillis());

        var userSaved = userRepository.save(user);

        br.ufpb.dsc.corrida.organizer.Organizer organizer = new br.ufpb.dsc.corrida.organizer.Organizer();
        organizer.setUsuario(userSaved);
        long timestampCref = System.currentTimeMillis();
        organizer.setCref((timestampCref % 100000) + "-G/PB");
        organizer.setEmail("org_" + timestamp + "@test.com");
        organizer.setWhatsapp("83999999999");
        organizer.setUfConselho("PB");

        String cpfDinamico = String.format("%011d", timestamp % 100000000000L);
        organizer.setCpf(cpfDinamico);

        organizer = organizerRepository.save(organizer);

        br.ufpb.dsc.corrida.organizer.Organization org = new br.ufpb.dsc.corrida.organizer.Organization();
        org.setName("Org Duplicada");
        org.setFoundedAt(java.time.LocalDate.now());
        org.setCity("João Pessoa");
        org.setState("PB");
        org.setOrganizer(organizer);
        org = organizationRepository.save(org);

        Race race = new Race();
        race.setNome(nome);
        race.setSlug("corrida-1-vaga-" + System.currentTimeMillis());
        race.setDescricao("Descrição padrão para testes de integração");
        race.setValorInscricao(valor);
        race.setDataInicio(java.time.OffsetDateTime.now().plusDays(10));
        race.setStatus(StatusCorrida.PUBLICADA);
        race.setCategoria(CategoriaCorrida.C5K);
        race.setLargadaLat(-7.1195);
        race.setLargadaLng(-34.8450);
        race.setLargadaEndereco("Av. Cabo Branco");
        race.setChegadaLat(-7.1195);
        race.setChegadaLng(-34.8450);
        race.setChegadaEndereco("Busto de Tamandaré");
        race.setOrganization(org);
        return raceRepository.save(race);
    }

    @Test
    @DisplayName("Banco de dados: deve proibir 2 inscrições em AGUARDANDO_PAGAMENTO para mesmo atleta/corrida via índice único parcial")
    void bancoDeDados_deveImpedirInscricaoDuplicadaAguardandoPagamento() {
        Race corrida = criarCorridaValida("Corrida Teste Indice", "corrida-teste-2026", new BigDecimal("40.00"));
        
        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        // IMPORTANTE: Deixar username, login e email dinâmicos para não estourar a chave única (UNIQUE)
        user.setLogin("user_" + timestamp + "@gmail.com");
        user.setUsername("atleta_" + System.currentTimeMillis());

        var userSaved = userRepository.save(user);

        Inscricao i1 = new Inscricao();
        i1.setUsuario(userSaved);
        i1.setCorrida(corrida);
        i1.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        inscricaoRepository.saveAndFlush(i1);

        Inscricao i2 = new Inscricao();
        i2.setUsuario(userSaved);
        i2.setCorrida(corrida);
        i2.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);

        assertThrows(DataIntegrityViolationException.class, () -> {
            inscricaoRepository.saveAndFlush(i2);
        });
    }

    @Test
    @DisplayName("Banco de dados: deve permitir nova inscrição após a primeira ter sido CANCELADA")
    void bancoDeDados_devePermitirReinscricaoAposCancelamento() {
        Race corrida = criarCorridaValida("Corrida de Teste", "corrida-de-teste", new BigDecimal("30.00"));

        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        // IMPORTANTE: Deixar username, login e email dinâmicos para não estourar a chave única (UNIQUE)
        user.setLogin("user_" + timestamp + "@gmail.com");
        user.setUsername("atleta_" + System.currentTimeMillis());

        var usuario = userRepository.save(user);

        Inscricao i1 = new Inscricao();
        i1.setUsuario(usuario);
        i1.setCorrida(corrida);
        i1.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        i1 = inscricaoRepository.saveAndFlush(i1);

        // Cancelar primeira
        i1.setStatus(StatusInscricao.CANCELADA);
        inscricaoRepository.saveAndFlush(i1);

        // Criar segunda inscrição em AGUARDANDO_PAGAMENTO
        Inscricao i2 = new Inscricao();
        i2.setUsuario(usuario);
        i2.setCorrida(corrida);
        i2.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);

        assertDoesNotThrow(() -> {
            inscricaoRepository.saveAndFlush(i2);
        });
    }
}
