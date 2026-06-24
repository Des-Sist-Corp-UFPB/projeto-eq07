package br.ufpb.dsc.corrida.race;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RaceRepository extends JpaRepository<Race, Long> {

    /**
     * Whitelist query: retorna apenas corridas cujo status esteja na lista fornecida.
     * Usado para o feed público (apenas PUBLICADA).
     */
    Page<Race> findAllByStatusInAndDataInicioAfter(List<StatusCorrida> statuses, OffsetDateTime data, Pageable pageable);

    /** Histórico público: apenas ENCERRADA. */
    List<Race> findAllByStatus(StatusCorrida status);

    /** Visão de gerenciamento do organizador: todos os status. */
    List<Race> findAllByOrganizationId(Long organizationId);

    /** Busca por slug para página de detalhe. */
    Optional<Race> findBySlug(String slug);
}
