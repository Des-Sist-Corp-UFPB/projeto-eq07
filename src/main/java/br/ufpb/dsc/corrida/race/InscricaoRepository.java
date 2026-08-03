package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    boolean existsByUsuarioAndCorridaAndStatus(User usuario, Race corrida, StatusInscricao status);

    java.util.Optional<Inscricao> findFirstByUsuarioAndCorridaAndStatusInOrderByIdDesc(User usuario, Race corrida, Collection<StatusInscricao> statuses);

    long countByCorridaAndStatus(Race corrida, StatusInscricao status);

    /**
     * Conta inscrições em múltiplos status — usado na Opção A de controle de capacidade:
     * reserva a vaga em AGUARDANDO_PAGAMENTO e também conta ATIVA (legado) e CONFIRMADA.
     */
    long countByCorridaAndStatusIn(Race corrida, Collection<StatusInscricao> statuses);

    @Query("SELECT COUNT(i) FROM Inscricao i " +
           "WHERE i.usuario.id = :userId " +
           "AND i.corrida.id <> :raceId " +
           "AND i.status = 'ATIVA' " +
           "AND i.corrida.dataInicio < :novoEndTime " +
           "AND i.corrida.dataFim > :novoStartTime")
    long countOverlappingInscricoes(
            @Param("userId") Long userId,
            @Param("raceId") Long raceId,
            @Param("novoStartTime") OffsetDateTime novoStartTime,
            @Param("novoEndTime") OffsetDateTime novoEndTime);

    List<Inscricao> findByUsuarioAndStatus(User usuario, StatusInscricao status);

    List<Inscricao> findByUsuarioAndStatusInOrderByIdDesc(User usuario, Collection<StatusInscricao> statuses);

    Page<Inscricao> findByCorridaId(Long corridaId, Pageable pageable);

    List<Inscricao> findByCorridaAndStatusAndCompareceuTrue(Race corrida, StatusInscricao status);

    /**
     * Para o job de expiração: busca inscrições aguardando pagamento
     * cujo pagamento já expirou.
     */
    @Query("SELECT i FROM Inscricao i JOIN i.pagamento p " +
           "WHERE i.status = :status AND p.expirationDate < :agora")
    List<Inscricao> findInscricoesExpiradas(
            @Param("status") StatusInscricao status,
            @Param("agora") OffsetDateTime agora);
}
