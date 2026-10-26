package ro.uaic.feaa.cloudguard.domain.model.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Mapează PortSet pe o coloană text ("22,443") și înapoi. */
@Converter
public class PortSetConverter implements AttributeConverter<PortSet, String> {

    @Override
    public String convertToDatabaseColumn(PortSet attribute) {
        return attribute == null ? "" : attribute.toString();
    }

    @Override
    public PortSet convertToEntityAttribute(String dbData) {
        return PortSet.parse(dbData);
    }
}
