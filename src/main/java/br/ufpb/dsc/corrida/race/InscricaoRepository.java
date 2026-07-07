package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    boolean existsByUsuarioAndCorridaAndStatus(User usuario, Race corrida, StatusInscricao status);

    long countByCorridaAndStatus(Race corrida, StatusInscricao status);

    @Query("SELECT COUNT(i) FROM Inscricao i " +
           "WHERE i.usuario.id = :userId " +
           "AND i.status = 'ATIVA' " +
           "AND i.corrida.dataInicio < :novoEndTime " +
           "AND i.corrida.dataFim > :novoStartTime")
    long countOverlappingInscricoes(
            @Param("userId") Long userId, 
            @Param("novoStartTime") OffsetDateTime novoStartTime, 
            @Param("novoEndTime") OffsetDateTime novoEndTime);

    List<Inscricao> findByUsuarioAndStatus(User usuario, StatusInscricao status);

    Page<Inscricao> findByCorridaId(Long corridaId, Pageable pageable);
    
    List<Inscricao> findByCorridaAndStatusAndCompareceuTrue(Race corrida, StatusInscricao status);
}
