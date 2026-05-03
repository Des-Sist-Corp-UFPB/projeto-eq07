package br.ufpb.dsc.corrida.dto.user;

import jakarta.validation.constraints.NotBlank;

public record RegistrarUsuarioDTO(
        @NotBlank
        String nome,
        @NotBlank
        String username,
        @NotBlank
        String login,
        @NotBlank
        String senha
) {}
