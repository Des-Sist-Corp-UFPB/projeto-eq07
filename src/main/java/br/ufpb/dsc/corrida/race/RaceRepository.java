package br.ufpb.dsc.corrida.race;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.ufpb.dsc.corrida.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT COUNT(r) FROM Race r " +
           "WHERE r.organization.organizer.usuario.id = :userId " +
           "AND r.status NOT IN ('CANCELADA', 'ENCERRADA') " +
           "AND r.dataInicio < :novoEndTime " +
           "AND r.dataFim > :novoStartTime")
    long countOverlappingOrganizedRaces(
            @Param("userId") Long userId, 
            @Param("novoStartTime") OffsetDateTime novoStartTime, 
            @Param("novoEndTime") OffsetDateTime novoEndTime);

    List<Race> findByOrganization_Organizer_Usuario(User usuario);

    /** Visão de gerenciamento do organizador: todos os status. */
    List<Race> findAllByOrganizationId(Long organizationId);

    /** Busca por slug para página de detalhe. */
    Optional<Race> findBySlug(String slug);
}
