package site.memberservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.memberservice.exception.MemberException;

import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static site.memberservice.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class PhoneNumber {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$");

    @Column(name = "phone_number", length = 20, nullable = false)
    private String value;

    public PhoneNumber(final String value) {
        validateValue(value);
        this.value = value;
    }

    private void validateValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 전화번호는 null 혹은 공백일 수 없습니다. input : %s", value));
        }

        if (!PHONE_NUMBER_PATTERN.matcher(value).matches()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("유효하지 않은 형식의 전화번호입니다. input : %s", value));
        }
    }

    // TODO : 전화번호 값 조회 케이스가 여러가지 이므로 필요할 때 추가함
    // ex) 구분자 제외한 번호 조회, 뒷자리만 조회, etc...

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
