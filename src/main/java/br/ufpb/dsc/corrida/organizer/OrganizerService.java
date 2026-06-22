package br.ufpb.dsc.corrida.organizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorCompletoDTO;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;

import java.util.Optional;

@Service
public class OrganizerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Transactional
    public User registrarOrganizador(RegistrarOrganizadorCompletoDTO dto) {
        if (userRepository.existsByLogin(dto.step1().login())) {
            throw new IllegalArgumentException("Login já utilizado, tente outro");
        }
        if (userRepository.existsByUsername(dto.step1().username())) {
            throw new IllegalArgumentException("Username já utilizado, tente outro");
        }

        User user = new User(dto.step1(), Papel.ORGANIZADOR);
        User savedUser = userRepository.save(user);

        Organizer organizer = new Organizer();
        organizer.setUsuario(savedUser);
        organizer.setCref(dto.step1().cref());
        organizer.setCpf(dto.step1().cpf());
        organizer.setEmail(dto.step1().email());
        organizer.setWhatsapp(dto.step1().whatsapp());
        organizer.setUfConselho(dto.step1().ufConselho());
        Organizer savedOrganizer = organizerRepository.save(organizer);

        Organization organization = new Organization();
        organization.setOrganizer(savedOrganizer);
        organization.setName(dto.step2().name());
        organization.setFoundedAt(dto.step2().foundedAt());
        organization.setDescription(dto.step2().description());
        organization.setLogoUrl(dto.step2().logoUrl());
        organization.setCity(dto.step2().city());
        organization.setState(dto.step2().state());
        organization.setSocialLink(dto.step2().socialLink());
        organizationRepository.save(organization);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public Organization buscarOrganizacaoPorId(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException("Organização não encontrada"));
    }

    @Transactional(readOnly = true)
    public Optional<Organizer> buscarOrganizadorPorUsuarioId(Long usuarioId) {
        return organizerRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public Optional<Organization> buscarOrganizacaoPorOrganizadorId(Long organizerId) {
        return organizationRepository.findByOrganizerId(organizerId);
    }
}
