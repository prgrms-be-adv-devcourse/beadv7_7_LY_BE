package site.memberservice.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.memberservice.member.exception.MemberException;

import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    @Column(name = "email", length = 255, nullable = false)
    private String value;

    public Email(final String value) {
        validateValue(value);
        this.value = value;
    }

    private void validateValue(final String value) {
        if (value == null || value.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 이메일은 null 혹은 공백일 수 없습니다. input : %s", value));
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("유효하지 않은 형식의 이메일입니다. input : %s", value));
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
