package br.ufpb.dsc.corrida.pagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.ufpb.dsc.corrida.race.StatusPagamento;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    /** Busca por ID do Mercado Pago — usado para reconciliação de webhook. */
    Optional<Pagamento> findByMpPaymentId(Long mpPaymentId);

    /** Busca por ID de inscrição. */
    Optional<Pagamento> findByInscricaoId(Long inscricaoId);

    /** Busca pagamentos pendentes com data de expiração passada — usado pelo job de limpeza. */
    List<Pagamento> findByStatusAndExpirationDateBefore(StatusPagamento status, OffsetDateTime data);
}
