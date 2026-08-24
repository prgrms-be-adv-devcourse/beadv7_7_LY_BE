package site.memberservice.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import site.common.crypto.KmsEncryptor;

@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final KmsEncryptor kmsEncryptor;

    public EncryptedStringConverter(final KmsEncryptor kmsEncryptor) {
        this.kmsEncryptor = kmsEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(final String attribute) {
        return attribute == null ? null : kmsEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(final String dbData) {
        return dbData == null ? null : kmsEncryptor.decrypt(dbData);
    }
}
