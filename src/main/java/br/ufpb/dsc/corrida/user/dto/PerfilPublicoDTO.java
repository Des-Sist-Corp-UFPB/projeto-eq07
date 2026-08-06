package br.ufpb.dsc.corrida.user.dto;

public record PerfilPublicoDTO(
    String nome,
    String username,
    String fotoPerfil,
    Float totalKmRun
) {
    public String getNome() {
        return nome;
    }

    public String getUsername() {
        return username;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public Float getTotalKmRun() {
        return totalKmRun;
    }
}