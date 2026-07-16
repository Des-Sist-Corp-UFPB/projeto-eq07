package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorridaValidationTest — Unit Tests")
public class CorridaValidationTest {

    private Validator validator;

    @Mock
    private RaceRepository raceRepository;

    @InjectMocks
    private CorridaService service;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
/** 
    @Test
    @DisplayName("Should fail validation if dataInicio is in the past")
    void shouldFailIfDataInicioInPast() {
        CriarCorridaDTO dto = new CriarCorridaDTO(
                "Corrida Teste",
                "Descrição da Corrida",
                "http://banner.url",
                BigDecimal.TEN,
                100,
                OffsetDateTime.now().minusDays(1), // Past
                CategoriaCorrida.C5K,
                -7.115,
                -34.863,
                "Largada",
                -7.115,
                -34.863,
                "Chegada",
                Set.of(BeneficioCorrida.AGUA)
        );

        Set<ConstraintViolation<CriarCorridaDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dataInicio"))).isTrue();
    }

    @Test
    @DisplayName("Should pass validation if dataInicio is in the future")
    void shouldPassIfDataInicioInFuture() {
        CriarCorridaDTO dto = new CriarCorridaDTO(
                "Corrida Teste",
                "Descrição da Corrida",
                "http://banner.url",
                BigDecimal.TEN,
                100,
                OffsetDateTime.now().plusDays(1), // Future
                CategoriaCorrida.C5K,
                -7.115,
                -34.863,
                "Largada",
                -7.115,
                -34.863,
                "Chegada",
                Set.of(BeneficioCorrida.AGUA)
        );

        Set<ConstraintViolation<CriarCorridaDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should generate slug properly from name and year")
    void shouldGenerateSlugProperly() {
        when(raceRepository.findBySlug("meia-maratona-do-sol-2026")).thenReturn(Optional.empty());

        String slug = service.gerarSlugUnico("Meia Maratona do Sol 2026!", 2026);
        assertThat(slug).isEqualTo("meia-maratona-do-sol-2026");
    }

    @Test
    @DisplayName("Should resolve slug collision by appending suffix")
    void shouldResolveSlugCollision() {
        when(raceRepository.findBySlug("corrida-de-teste-2026")).thenReturn(Optional.of(new Race()));
        when(raceRepository.findBySlug("corrida-de-teste-2026-2")).thenReturn(Optional.empty());

        String slug = service.gerarSlugUnico("Corrida de Teste 2026", 2026);
        assertThat(slug).isEqualTo("corrida-de-teste-2026-2");
    }
        */
}