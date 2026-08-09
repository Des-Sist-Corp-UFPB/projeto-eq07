package br.ufpb.dsc.corrida.race.dto;

import br.ufpb.dsc.corrida.race.BeneficioCorrida;
import br.ufpb.dsc.corrida.race.CategoriaCorrida;
import br.ufpb.dsc.corrida.race.Terreno;
import br.ufpb.dsc.corrida.race.ClimaEsperado;
import br.ufpb.dsc.corrida.race.NivelDificuldade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO de edição de corrida.
 *
 * <p>Inclui todos os campos mutáveis. O serviço compara coordenadas com os
 * valores persistidos para decidir se deve re-chamar o ORS.
 */
public record EditarCorridaDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
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

        @NotNull Double largadaLat,
        @NotNull Double largadaLng,
        @NotBlank String largadaEndereco,

        @NotNull Double chegadaLat,
        @NotNull Double chegadaLng,
        @NotBlank String chegadaEndereco,

        Set<BeneficioCorrida> beneficios,

        Terreno terreno,

        Integer ganhoElevacao,

        ClimaEsperado climaEsperado,

        NivelDificuldade nivelDificuldade
) {}
