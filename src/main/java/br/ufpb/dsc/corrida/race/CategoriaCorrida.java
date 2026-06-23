package br.ufpb.dsc.corrida.race;

/**
 * Categoria (distância-padrão) da corrida.
 */
public enum CategoriaCorrida {
    C5K("5 km"),
    C10K("10 km"),
    C21K("Meia Maratona (21 km)"),
    C42K("Maratona (42 km)"),
    OUTRO("Outra distância");

    private final String label;

    CategoriaCorrida(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
