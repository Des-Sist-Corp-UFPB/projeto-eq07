package br.ufpb.dsc.corrida.dto.user;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank
        String login,
        @NotBlank
        String senha
) {}
