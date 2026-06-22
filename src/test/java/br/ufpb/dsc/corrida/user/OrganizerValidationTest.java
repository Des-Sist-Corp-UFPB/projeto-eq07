package br.ufpb.dsc.corrida.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep1DTO;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep2DTO;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Organizer DTO Validation — Unit Tests")
class OrganizerValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Step 1 DTO — should pass validation when all fields are valid")
    void step1_shouldPass_whenFieldsAreValid() {
        RegistrarOrganizadorStep1DTO dto = new RegistrarOrganizadorStep1DTO(
                "João da Silva",
                "joao_organizador",
                "joao@organizador.com",
                "senha123",
                "123456-G/SP",
                "123.456.789-00",
                "joao@organizador.com",
                "11999999999",
                "SP"
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep1DTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Step 1 DTO — should fail when CPF format is invalid")
    void step1_shouldFail_whenCpfInvalid() {
        RegistrarOrganizadorStep1DTO dto = new RegistrarOrganizadorStep1DTO(
                "João da Silva", "joao_org", "joao@org.com", "senha123",
                "123456-G/SP", "123.abc.789-00", "joao@org.com", "11999999999", "SP"
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep1DTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cpf"))).isTrue();
    }

    @Test
    @DisplayName("Step 1 DTO — should fail when CREF format is invalid")
    void step1_shouldFail_whenCrefInvalid() {
        RegistrarOrganizadorStep1DTO dto = new RegistrarOrganizadorStep1DTO(
                "João da Silva", "joao_org", "joao@org.com", "senha123",
                "123456-G", "123.456.789-00", "joao@org.com", "11999999999", "SP"
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep1DTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cref"))).isTrue();
    }

    @Test
    @DisplayName("Step 1 DTO — should fail when UF format is invalid")
    void step1_shouldFail_whenUfInvalid() {
        RegistrarOrganizadorStep1DTO dto = new RegistrarOrganizadorStep1DTO(
                "João da Silva", "joao_org", "joao@org.com", "senha123",
                "123456-G/SP", "123.456.789-00", "joao@org.com", "11999999999", "sp" // lowercase should fail if regex is ^[A-Z]{2}$
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep1DTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ufConselho"))).isTrue();
    }

    @Test
    @DisplayName("Step 2 DTO — should pass validation when all fields are valid")
    void step2_shouldPass_whenFieldsAreValid() {
        RegistrarOrganizadorStep2DTO dto = new RegistrarOrganizadorStep2DTO(
                "Super Corridas LTDA",
                LocalDate.of(2020, 1, 1),
                "Organização líder em corridas de rua",
                "http://logo.com/logo.png",
                "São Paulo",
                "SP",
                "http://instagram.com/supercorridas"
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep2DTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Step 2 DTO — should fail validation when name is blank")
    void step2_shouldFail_whenNameIsBlank() {
        RegistrarOrganizadorStep2DTO dto = new RegistrarOrganizadorStep2DTO(
                "", // Blank name should fail
                LocalDate.of(2020, 1, 1),
                "Organização",
                "http://logo.com/logo.png",
                "São Paulo",
                "SP",
                "http://instagram.com/supercorridas"
        );

        Set<ConstraintViolation<RegistrarOrganizadorStep2DTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name"))).isTrue();
    }
}
