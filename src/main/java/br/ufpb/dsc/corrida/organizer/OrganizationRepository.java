package br.ufpb.dsc.corrida.organizer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrganizerId(Long organizerId);
}
