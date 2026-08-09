package br.ufpb.dsc.corrida.pagamento;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.StatusInscricao;
import br.ufpb.dsc.corrida.race.StatusPagamento;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job agendado que expira cobranças Pix vencidas e libera as vagas reservadas.
 *
 * <p>Executa a cada 10 minutos. Busca inscrições em AGUARDANDO_PAGAMENTO
 * cujo pagamento associado já ultrapassou a data de expiração e as cancela,
 * liberando a vaga para novos inscritos (Opção A de controle de capacidade).
 */
@Slf4j
@Component
public class PagamentoExpiracaoJob {

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void expirarCobrancasVencidas() {
        OffsetDateTime agora = OffsetDateTime.now();
        List<Inscricao> expiradas = inscricaoRepository.findInscricoesExpiradas(
                StatusInscricao.AGUARDANDO_PAGAMENTO, agora);

        if (expiradas.isEmpty()) {
            log.debug("[ExpiracaoJob] Nenhuma cobrança expirada encontrada.");
            return;
        }

        log.info("[ExpiracaoJob] Expirando {} inscrição(ões).", expiradas.size());

        for (Inscricao inscricao : expiradas) {
            inscricao.setStatus(StatusInscricao.CANCELADA);
            if (inscricao.getPagamento() != null) {
                inscricao.getPagamento().setStatus(StatusPagamento.EXPIRADO);
            }
            inscricaoRepository.save(inscricao);
            log.info("[ExpiracaoJob] inscricaoId={} expirada.", inscricao.getId());
        }

        log.info("[ExpiracaoJob] {} inscrição(ões) expirada(s) com sucesso.", expiradas.size());
    }
}
