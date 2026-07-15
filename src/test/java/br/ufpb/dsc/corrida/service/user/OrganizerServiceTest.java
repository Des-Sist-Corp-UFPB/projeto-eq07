package br.ufpb.dsc.corrida.service.user;

import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizationRepository;
import br.ufpb.dsc.corrida.organizer.Organizer;
import br.ufpb.dsc.corrida.organizer.OrganizerRepository;
import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorCompletoDTO;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep1DTO;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep2DTO;
import br.ufpb.dsc.corrida.user.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizerService — Unit Tests")
class OrganizerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizerRepository organizerRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizerService organizerService;

    private RegistrarOrganizadorCompletoDTO completeDto;

    @BeforeEach
    void setUp() {
        RegistrarOrganizadorStep1DTO step1 = new RegistrarOrganizadorStep1DTO(
                "João da Silva", "joao_org", "joao@org.com", "senha123",
                "123456-G/SP", "123.456.789-00", "joao@org.com", "11999999999", "SP"
        );

        RegistrarOrganizadorStep2DTO step2 = new RegistrarOrganizadorStep2DTO(
                "Super Corridas LTDA", LocalDate.of(2020, 1, 1),
                "Organização", "logo.png", "São Paulo", "SP", "social"
        );

        completeDto = new RegistrarOrganizadorCompletoDTO(step1, step2);
    }

    @Test
    @DisplayName("registrarOrganizador() — should register user as ORGANIZER, save organizer profile and organization")
    void registrar_shouldCreateOrganizerAndOrganization_withOrganizerRole() {
        // Arrange
        User mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "nome", "João da Silva");
        ReflectionTestUtils.setField(mockUser, "papel", Papel.ORGANIZADOR);

        when(userRepository.existsByLogin(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        Organizer mockOrganizer = new Organizer();
        mockOrganizer.setId(10L);
        mockOrganizer.setUsuario(mockUser);
        mockOrganizer.setCref(completeDto.step1().cref());
        mockOrganizer.setCpf(completeDto.step1().cpf());

        when(organizerRepository.save(any(Organizer.class))).thenReturn(mockOrganizer);

        Organization mockOrg = new Organization();
        mockOrg.setId(100L);
        mockOrg.setOrganizer(mockOrganizer);
        mockOrg.setName(completeDto.step2().name());

        when(organizationRepository.save(any(Organization.class))).thenReturn(mockOrg);

        // Act
        User result = organizerService.registrarOrganizador(completeDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPapel()).isEqualTo(Papel.ORGANIZADOR);
        verify(userRepository).save(any(User.class));
        verify(organizerRepository).save(any(Organizer.class));
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("registrarOrganizador() — should throw exception if username already exists")
    void registrar_shouldThrowException_whenUsernameExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> organizerService.registrarOrganizador(completeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username já utilizado");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("buscarOrganizacaoPorId() — should find organization if exists")
    void buscarOrganizacaoPorId_shouldReturnOrganization_whenExists() {
        Organization mockOrg = new Organization();
        mockOrg.setId(1L);
        mockOrg.setName("Org Test");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(mockOrg));

        Organization result = organizerService.buscarOrganizacaoPorId(1L);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Org Test");
    }
}
