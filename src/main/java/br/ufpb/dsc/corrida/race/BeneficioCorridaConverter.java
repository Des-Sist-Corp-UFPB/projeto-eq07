package br.ufpb.dsc.corrida.race;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BeneficioCorridaConverter implements AttributeConverter<BeneficioCorrida, String> {

    @Override
    public String convertToDatabaseColumn(BeneficioCorrida beneficio) {
        return beneficio == null ? null : beneficio.name();
    }

    @Override
    public BeneficioCorrida convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BeneficioCorrida.valueOf(dbData);
    }
}