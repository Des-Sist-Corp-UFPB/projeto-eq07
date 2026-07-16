package br.ufpb.dsc.corrida.race;

/**
 * Enum que representa o nível de dificuldade de uma corrida.
 */
public enum NivelDificuldade {
    FACIL("Fácil"),
    MEDIO("Médio"),
    DIFICIL("Difícil"),
    EXTREMO("Extremo");

    private final String label;

    NivelDificuldade(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
