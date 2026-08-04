package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.CpfObrigatorioException;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.InscricaoService;
import br.ufpb.dsc.corrida.inscricao.StatusInscricao;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoServicePaymentTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @InjectMocks
    private InscricaoService inscricaoService;

    private User usuario;
    private UserInfo userInfo;
    private Race corridaPaga;
    private Race corridaGratuita;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(1L);
        usuario.setLogin("atleta@teste.com");

        userInfo = new UserInfo();
        userInfo.setId(10L);
        userInfo.setUsuario(usuario);
        userInfo.setCpf("52998224725");

        corridaPaga = new Race();
        corridaPaga.setId(100L);
        corridaPaga.setNome("Maratona DSC");
        corridaPaga.setValorInscricao(new BigDecimal("50.00"));
        corridaPaga.setDataInicio(OffsetDateTime.now().plusDays(10));
        corridaPaga.setStatus(StatusCorrida.PUBLICADA);

        corridaGratuita = new Race();
        corridaGratuita.setId(200L);
        corridaGratuita.setNome("Corrida Livre");
        corridaGratuita.setValorInscricao(BigDecimal.ZERO);
        corridaGratuita.setDataInicio(OffsetDateTime.now().plusDays(10));
        corridaGratuita.setStatus(StatusCorrida.PUBLICADA);
    }

    @Test
    @DisplayName("Corrida gratuita: deve confirmar inscrição diretamente sem exigir CPF ou MP")
    void corridaGratuita_deveConfirmarDireto() {
        when(raceRepository.findById(200L)).thenReturn(Optional.of(corridaGratuita));
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscricao insc = inscricaoService.inscrever(usuario, 200L, false);

        assertEquals(StatusInscricao.CONFIRMADA, insc.getStatus());
        verify(mercadoPagoService, never()).criarCobrancaPix(any());
        verify(userInfoRepository, never()).findByUsuarioId(any());
    }

    @Test
    @DisplayName("Corrida paga sem CPF: deve lançar CpfObrigatorioException e não chamar MP")
    void corridaPaga_semCpf_deveLancarExcecao() {
        when(raceRepository.findById(100L)).thenReturn(Optional.of(corridaPaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(CpfObrigatorioException.class, () -> inscricaoService.inscrever(usuario, 100L, false));
        verify(mercadoPagoService, never()).criarCobrancaPix(any());
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Corrida paga com CPF: deve criar inscrição AGUARDANDO_PAGAMENTO e chamar MP")
    void corridaPaga_comCpf_deveCriarCobrancaPix() {
        when(raceRepository.findById(100L)).thenReturn(Optional.of(corridaPaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(userInfo));
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(inv -> {
            Inscricao i = inv.getArgument(0);
            if (i.getId() == null) i.setId(500L);
            return i;
        });

        Inscricao insc = inscricaoService.inscrever(usuario, 100L, false);

        assertEquals(StatusInscricao.AGUARDANDO_PAGAMENTO, insc.getStatus());
        verify(mercadoPagoService, times(1)).criarCobrancaPix(any(Inscricao.class));
    }

    @Test
    @DisplayName("Falha na API do Mercado Pago: deve marcar inscrição como CANCELADA e propagar exceção")
    void corridaPaga_falhaMP_deveCancelarInscricao() {
        when(raceRepository.findById(100L)).thenReturn(Optional.of(corridaPaga));
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(userInfo));
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(inv -> {
            Inscricao i = inv.getArgument(0);
            if (i.getId() == null) i.setId(500L);
            return i;
        });
        doThrow(new MercadoPagoException("Erro API MP"))
                .when(mercadoPagoService).criarCobrancaPix(any());

        assertThrows(MercadoPagoException.class, () -> inscricaoService.inscrever(usuario, 100L, false));

        // Verifica se save foi chamado para alterar status para CANCELADA
        verify(inscricaoRepository, atLeast(2)).save(argThat(i ->
                i.getStatus() == StatusInscricao.CANCELADA
        ));
    }
}
