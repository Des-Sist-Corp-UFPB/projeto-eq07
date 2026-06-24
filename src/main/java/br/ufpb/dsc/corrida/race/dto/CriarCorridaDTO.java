package br.ufpb.dsc.corrida.race.dto;

import br.ufpb.dsc.corrida.race.BeneficioCorrida;
import br.ufpb.dsc.corrida.race.CategoriaCorrida;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO de criação de corrida.
 *
 * <p>Todos os campos obrigatórios são validados com Bean Validation.
 * As coordenadas são preenchidas pelo autocomplete de geocodificação no front-end.
 */
public record CriarCorridaDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Descrição é obrigatória")
        String descricao,

        String bannerUrl,

        BigDecimal valorInscricao,

        Integer maxInscricoes,

        @NotNull(message = "Data de início é obrigatória")
        @Future(message = "A data de início deve ser no futuro")
        OffsetDateTime dataInicio,

        @NotNull(message = "Categoria é obrigatória")
        CategoriaCorrida categoria,

        @NotNull(message = "Latitude de largada é obrigatória")
        Double largadaLat,

        @NotNull(message = "Longitude de largada é obrigatória")
        Double largadaLng,

        @NotBlank(message = "Endereço de largada é obrigatório")
        String largadaEndereco,

        @NotNull(message = "Latitude de chegada é obrigatória")
        Double chegadaLat,

        @NotNull(message = "Longitude de chegada é obrigatória")
        Double chegadaLng,

        @NotBlank(message = "Endereço de chegada é obrigatório")
        String chegadaEndereco,

        Set<BeneficioCorrida> beneficios
) {}
