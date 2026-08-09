package br.ufpb.dsc.corrida.race;

/**
 * Enum que representa as categorias de terreno de uma corrida.
 */
public enum Terreno {
    ASFALTO("Asfalto"),
    TRILHA("Trilha"),
    AREIA("Areia"),
    MISTO("Misto");

    private final String label;

    Terreno(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
