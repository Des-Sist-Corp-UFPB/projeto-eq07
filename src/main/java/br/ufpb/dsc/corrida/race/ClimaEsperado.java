package br.ufpb.dsc.corrida.race;

/**
 * Enum que representa as condições de clima esperado para uma corrida.
 */
public enum ClimaEsperado {
    AMENO("Ameno"),
    CALOR_INTENSO("Calor Intenso"),
    FRIO_INTENSO("Frio Intenso"),
    CHUVOSO("Chuvoso"),
    UMIDADE_ALTA("Umidade Alta");

    private final String label;

    ClimaEsperado(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
