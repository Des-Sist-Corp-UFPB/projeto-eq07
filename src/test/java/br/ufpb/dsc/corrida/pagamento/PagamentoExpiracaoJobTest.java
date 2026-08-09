package br.ufpb.dsc.corrida.pagamento;

import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.StatusInscricao;
import br.ufpb.dsc.corrida.race.StatusPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoExpiracaoJob — Unit Tests")
class PagamentoExpiracaoJobTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @InjectMocks
    private PagamentoExpiracaoJob job;

    @Test
    @DisplayName("expirarCobrancasVencidas() — quando não há inscrições expiradas, não altera nada")
    void expirarCobrancasVencidas_semExpiradas() {
        when(inscricaoRepository.findInscricoesExpiradas(eq(StatusInscricao.AGUARDANDO_PAGAMENTO), any()))
                .thenReturn(Collections.emptyList());

        job.expirarCobrancasVencidas();

        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("expirarCobrancasVencidas() — cancela inscrições expiradas e atualiza status do pagamento")
    void expirarCobrancasVencidas_comExpiradas() {
        Inscricao inscricao = new Inscricao();
        inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        Pagamento pagamento = new Pagamento();
        pagamento.setStatus(StatusPagamento.PENDENTE);
        inscricao.setPagamento(pagamento);

        when(inscricaoRepository.findInscricoesExpiradas(eq(StatusInscricao.AGUARDANDO_PAGAMENTO), any()))
                .thenReturn(List.of(inscricao));

        job.expirarCobrancasVencidas();

        assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.EXPIRADO);
        verify(inscricaoRepository, times(1)).save(inscricao);
    }
}
