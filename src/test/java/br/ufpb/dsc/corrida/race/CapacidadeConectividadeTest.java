package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.race.CorridaCheiaException;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.InscricaoService;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
import br.ufpb.dsc.corrida.pagamento.Pagamento;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CapacidadeConectividadeTest {

    @Autowired
    private InscricaoService inscricaoService;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @MockitoBean
    private MercadoPagoService mercadoPagoService;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizationRepository organizationRepository;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizerRepository organizerRepository;

    private Race corrida1Vaga;
    private User user1;
    private User user2;

    private Race criarCorridaValida(String nome, String slug, BigDecimal valor, Integer maxInscricoes) {
        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        // IMPORTANTE: Deixar username, login e email dinÃ¢micos para nÃ£o estourar a chave Ãºnica (UNIQUE)
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
        org.setName("Org Concorrencia");
        org.setFoundedAt(java.time.LocalDate.now());
        org.setCity("JoÃ£o Pessoa");
        org.setState("PB");
        org.setOrganizer(organizer);
        org = organizationRepository.save(org);

        Race race = new Race();
        race.setNome(nome);
        race.setSlug("corrida-1-vaga-" + System.currentTimeMillis());
        race.setDescricao("DescriÃ§Ã£o teste para concorrÃªncia");
        race.setValorInscricao(valor);
        race.setMaxInscricoes(maxInscricoes);
        race.setDataInicio(OffsetDateTime.now().plusDays(15));
        race.setStatus(StatusCorrida.PUBLICADA);
        race.setCategoria(CategoriaCorrida.C5K);
        race.setLargadaLat(-7.1195);
        race.setLargadaLng(-34.8450);
        race.setLargadaEndereco("Av. Cabo Branco");
        race.setChegadaLat(-7.1195);
        race.setChegadaLng(-34.8450);
        race.setChegadaEndereco("Busto de TamandarÃ©");
        race.setOrganization(org);
        return raceRepository.save(race);
    }

    @BeforeEach
    void setUp() {
        inscricaoRepository.deleteAll();

        corrida1Vaga = criarCorridaValida(
                "Corrida 1 Vaga Concorrente",
                "corrida-1-vaga-" + System.currentTimeMillis(),
                new BigDecimal("25.00"),
                1
        );

        long timestamp = System.currentTimeMillis();

        // 1Âº USUÃRIO
        User user = new User();
        user.setNome("Peterson Treinador teste 1");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");
        user.setLogin("user_1_" + timestamp + "@gmail.com");
        user.setUsername("atleta_1_" + timestamp);
        this.user1 = userRepository.save(user);

        // CPF DinÃ¢mico 1 para evitar a UNIQUE constraint user_info_cpf_uk
        String cpfDinamico1 = String.format("%011d", (timestamp % 100000000000L));

        UserInfo u1Info = new UserInfo();
        u1Info.setUsuario(this.user1);
        u1Info.setCpf(cpfDinamico1); // <-- ALTERADO AQUI
        u1Info.setPeso(70f);
        u1Info.setAltura(175f);
        u1Info.setGenero(br.ufpb.dsc.corrida.user.Genero.MALE);
        u1Info.setDataNasc(java.time.LocalDate.of(1990, 1, 1));
        userInfoRepository.save(u1Info);

        long timestamp2 = System.currentTimeMillis() + 15;

        // 2Âº USUÃRIO
        User user2 = new User();
        user2.setNome("Peterson Treinador teste 2");
        user2.setPapel(Papel.ORGANIZADOR);
        user2.setSenha("12345678");
        user2.setLogin("user_2_" + timestamp2 + "@gmail.com");
        user2.setUsername("atleta_2_" + timestamp2);
        this.user2 = userRepository.save(user2);

        // CPF DinÃ¢mico 2
        String cpfDinamico2 = String.format("%011d", (timestamp2 % 100000000000L));

        UserInfo u2Info = new UserInfo();
        u2Info.setUsuario(this.user2);
        u2Info.setCpf(cpfDinamico2); // <-- ALTERADO AQUI
        u2Info.setPeso(65f);
        u2Info.setAltura(170f);
        u2Info.setGenero(br.ufpb.dsc.corrida.user.Genero.FEMALE);
        u2Info.setDataNasc(java.time.LocalDate.of(1992, 2, 2));
        userInfoRepository.save(u2Info);

        Pagamento pSimulado = new Pagamento();
        pSimulado.setAmount(new BigDecimal("25.00"));
        pSimulado.setStatus(StatusPagamento.PENDENTE);
        when(mercadoPagoService.criarCobrancaPix(any())).thenReturn(pSimulado);
    }

    @Test
    @DisplayName("ConcorrÃªncia: Duas requisiÃ§Ãµes simultÃ¢neas tentando a Ãºltima vaga â€” exatamente uma deve ter sucesso")
    void concorrencia_duasRequisicoes_apenasUmaDeveSucesso() throws InterruptedException {
        int threads = 2;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasCheias = new AtomicInteger(0);

        Long raceId = corrida1Vaga.getId();

        service.execute(() -> {
            try {
                latch.await();
                inscricaoService.inscrever(user1, raceId, false);
                sucessos.incrementAndGet();
            } catch (CorridaCheiaException e) {
                falhasCheias.incrementAndGet();
            } catch (Exception e) {
                // Outro erro
            } finally {
                finishLatch.countDown();
            }
        });

        service.execute(() -> {
            try {
                latch.await();
                inscricaoService.inscrever(user2, raceId, false);
                sucessos.incrementAndGet();
            } catch (CorridaCheiaException e) {
                falhasCheias.incrementAndGet();
            } catch (Exception e) {
                // Outro erro
            } finally {
                finishLatch.countDown();
            }
        });

        latch.countDown(); // Start both threads at the exact same moment
        finishLatch.await(); // Wait for both to finish
        service.shutdown();

        assertEquals(1, sucessos.get(), "Exatamente 1 inscriÃ§Ã£o deve ter sucesso");
        assertEquals(1, falhasCheias.get(), "Exatamente 1 deve falhar por CorridaCheiaException");
    }
}

