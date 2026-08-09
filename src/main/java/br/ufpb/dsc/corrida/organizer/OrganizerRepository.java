package br.ufpb.dsc.corrida.organizer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    Optional<Organizer> findByUsuarioId(Long usuarioId);
    Optional<Organizer> findByUsuarioUsername(String username);
}
