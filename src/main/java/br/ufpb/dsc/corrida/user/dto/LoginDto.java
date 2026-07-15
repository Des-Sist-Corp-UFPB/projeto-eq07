package br.ufpb.dsc.corrida.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank String login,
        @NotBlank String senha
) {}
