package br.ufpb.dsc.corrida.organizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistrarOrganizadorCompletoDTO(
        @NotNull @Valid RegistrarOrganizadorStep1DTO step1,
        @NotNull @Valid RegistrarOrganizadorStep2DTO step2
) {}
