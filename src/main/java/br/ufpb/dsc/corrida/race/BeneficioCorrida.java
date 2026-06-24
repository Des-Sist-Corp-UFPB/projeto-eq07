package br.ufpb.dsc.corrida.race;

/**
 * Benefícios/kits oferecidos ao participante.
 */
public enum BeneficioCorrida {
    AGUA("Água"),
    CAMISA("Camiseta"),
    MEDALHA("Medalha"),
    PRONTO_ATENDIMENTO("Pronto-atendimento"),
    CHIP_CRONOMETRAGEM("Chip de Cronometragem"),
    KIT_LARGADA("Kit de Largada"),
    FRUTAS("Frutas"),
    OUTRO("Outro");

    private final String label;

    BeneficioCorrida(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
