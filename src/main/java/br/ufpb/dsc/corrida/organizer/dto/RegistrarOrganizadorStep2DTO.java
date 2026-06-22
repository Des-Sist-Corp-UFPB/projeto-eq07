package br.ufpb.dsc.corrida.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegistrarOrganizadorStep2DTO(
        @NotBlank(message = "Nome da organização é obrigatório")
        String name,

        @NotNull(message = "Data de fundação é obrigatória")
        LocalDate foundedAt,

        String description,
        String logoUrl,
        String city,
        String state,
        String socialLink
) {}
