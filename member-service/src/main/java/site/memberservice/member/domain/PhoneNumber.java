package site.memberservice.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.memberservice.global.crypto.EncryptedStringConverter;
import site.memberservice.member.exception.MemberException;

import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class PhoneNumber {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$");

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_number", length = 500, nullable = false)
    private String value;

    @Column(name = "phone_number_hash", length = 64, nullable = false)
    private String hash;

    public PhoneNumber(final String value, final String hash) {
        validateValue(value);
        validateHash(hash);
        this.value = value;
        this.hash = hash;
    }

    private void validateValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 전화번호는 null 혹은 공백일 수 없습니다. input : %s", value));
        }

        if (!PHONE_NUMBER_PATTERN.matcher(value).matches()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("유효하지 않은 형식의 전화번호입니다. input : %s", value));
        }
    }

    private void validateHash(final String hash) {
        if (hash == null || hash.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, "회원 전화번호 해시는 null 혹은 공백일 수 없습니다.");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final PhoneNumber that = (PhoneNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
