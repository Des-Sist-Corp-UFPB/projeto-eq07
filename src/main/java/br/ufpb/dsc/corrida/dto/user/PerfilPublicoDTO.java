package br.ufpb.dsc.corrida.dto.user;

public record PerfilPublicoDTO(
    String nome,
    String username,
    String fotoPerfil,
    Float totalKmRun
) {}
