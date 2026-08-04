package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.event.RaceCompletedEvent;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.exception.CpfObrigatorioException;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.exception.race.ConflitoHorarioException;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
import br.ufpb.dsc.corrida.pagamento.Pagamento;
import br.ufpb.dsc.corrida.pagamento.PagamentoRepository;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.race.StatusCorrida;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscricaoService — Business Rules Coverage Tests")
class InscricaoServiceCoverageTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private InscricaoService inscricaoService;

    private User user;
    private Race racePaga;
    private Race raceGratuita;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        racePaga = new Race();
        racePaga.setId(10L);
        racePaga.setStatus(StatusCorrida.PUBLICADA);
        racePaga.setDataInicio(OffsetDateTime.now().plusDays(5));
        racePaga.setValorInscricao(new BigDecimal("50.00"));

        raceGratuita = new Race();
        raceGratuita.setId(20L);
        raceGratuita.setStatus(StatusCorrida.PUBLICADA);
        raceGratuita.setDataInicio(OffsetDateTime.now().plusDays(5));
        raceGratuita.setValorInscricao(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("inscrever() — lança CorridaNaoEncontradaException se corrida não existir")
    void inscrever_corridaNaoEncontrada() {
        when(raceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscricaoService.inscrever(user, 99L, false))
                .isInstanceOf(CorridaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("inscrever() — lança CpfObrigatorioException se corrida for paga e atleta não tiver CPF")
    void inscrever_corridaPagaSemCpf() {
        when(raceRepository.findById(10L)).thenReturn(Optional.of(racePaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscricaoService.inscrever(user, 10L, false))
                .isInstanceOf(CpfObrigatorioException.class);
    }

    @Test
    @DisplayName("inscrever() — realiza cobrança Pix em corrida paga e define status AGUARDANDO_PAGAMENTO")
    void inscrever_corridaPagaComCpf() {
        UserInfo userInfo = new UserInfo();
        userInfo.setCpf("12345678901");

        when(raceRepository.findById(10L)).thenReturn(Optional.of(racePaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(userInfo));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(any(), any(), any())).thenReturn(false);
        when(inscricaoRepository.findByUsuarioAndStatus(any(), any())).thenReturn(List.of());
        when(raceRepository.findByOrganization_Organizer_Usuario(any())).thenReturn(List.of());
        when(inscricaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Inscricao resultado = inscricaoService.inscrever(user, 10L, false);

        assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.AGUARDANDO_PAGAMENTO);
        verify(mercadoPagoService).criarCobrancaPix(any());
    }

    @Test
    @DisplayName("inscrever() — se MercadoPago falhar ao criar Pix, cancela inscrição e lança exceção")
    void inscrever_mercadoPagoFalha() {
        UserInfo userInfo = new UserInfo();
        userInfo.setCpf("12345678901");

        when(raceRepository.findById(10L)).thenReturn(Optional.of(racePaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(userInfo));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(any(), any(), any())).thenReturn(false);
        when(inscricaoRepository.findByUsuarioAndStatus(any(), any())).thenReturn(List.of());
        when(raceRepository.findByOrganization_Organizer_Usuario(any())).thenReturn(List.of());
        when(inscricaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MercadoPagoException("Erro MP")).when(mercadoPagoService).criarCobrancaPix(any());

        assertThatThrownBy(() -> inscricaoService.inscrever(user, 10L, false))
                .isInstanceOf(MercadoPagoException.class);

        verify(inscricaoRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("inscrever() — lança ConflitoHorarioException quando há conflito com corrida organizada pelo usuário")
    void inscrever_conflitoCorridaOrganizada() {
        when(raceRepository.findById(20L)).thenReturn(Optional.of(raceGratuita));
        when(inscricaoRepository.existsByUsuarioAndCorridaAndStatus(any(), any(), any())).thenReturn(false);
        when(inscricaoRepository.findByUsuarioAndStatus(any(), any())).thenReturn(List.of());

        Race orgRace = new Race();
        orgRace.setId(30L);
        orgRace.setStatus(StatusCorrida.PUBLICADA);
        orgRace.setDataInicio(raceGratuita.getDataInicio().plusMinutes(10));
        orgRace.setDuracaoEstimadaMin(120);

        when(raceRepository.findByOrganization_Organizer_Usuario(user)).thenReturn(List.of(orgRace));

        assertThatThrownBy(() -> inscricaoService.inscrever(user, 20L, false))
                .isInstanceOf(ConflitoHorarioException.class);
    }

    @Test
    @DisplayName("cancelar() — lança AcessoNaoPermitidoException se a inscrição for de outro usuário")
    void cancelar_acessoNegado() {
        User outroUser = new User();
        outroUser.setId(2L);

        Inscricao inscricao = new Inscricao();
        inscricao.setId(100L);
        inscricao.setUsuario(outroUser);

        when(inscricaoRepository.findById(100L)).thenReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> inscricaoService.cancelar(user, 100L))
                .isInstanceOf(AcessoNaoPermitidoException.class);
    }

    @Test
    @DisplayName("cancelar() — remove o registro de pagamento associado se existir")
    void cancelar_comPagamento() {
        Inscricao inscricao = new Inscricao();
        inscricao.setId(100L);
        inscricao.setUsuario(user);

        Pagamento pagamento = new Pagamento();
        pagamento.setId(50L);
        inscricao.setPagamento(pagamento);

        when(inscricaoRepository.findById(100L)).thenReturn(Optional.of(inscricao));

        inscricaoService.cancelar(user, 100L);

        assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(inscricao.getPagamento()).isNull();
        verify(pagamentoRepository).delete(pagamento);
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    @DisplayName("marcarPresenca() — altera compareceu e salva inscrição se for o organizador da corrida")
    void marcarPresenca_sucesso() {
        Organizer organizer = new Organizer();
        organizer.setId(5L);
        organizer.setUsuario(user);

        Organization organization = new Organization();
        organization.setOrganizer(organizer);

        Race race = new Race();
        race.setOrganization(organization);

        Inscricao inscricao = new Inscricao();
        inscricao.setId(200L);
        inscricao.setCorrida(race);

        when(inscricaoRepository.findById(200L)).thenReturn(Optional.of(inscricao));

        inscricaoService.marcarPresenca(user, 200L, true);

        assertThat(inscricao.isCompareceu()).isTrue();
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    @DisplayName("marcarPresenca() — lança AcessoNaoPermitidoException se não for o organizador da corrida")
    void marcarPresenca_acessoNegado() {
        User outroUser = new User();
        outroUser.setId(99L);

        Organizer organizer = new Organizer();
        organizer.setId(5L);
        organizer.setUsuario(outroUser);

        Organization organization = new Organization();
        organization.setOrganizer(organizer);

        Race race = new Race();
        race.setOrganization(organization);

        Inscricao inscricao = new Inscricao();
        inscricao.setId(200L);
        inscricao.setCorrida(race);

        when(inscricaoRepository.findById(200L)).thenReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> inscricaoService.marcarPresenca(user, 200L, true))
                .isInstanceOf(AcessoNaoPermitidoException.class);
    }
}
