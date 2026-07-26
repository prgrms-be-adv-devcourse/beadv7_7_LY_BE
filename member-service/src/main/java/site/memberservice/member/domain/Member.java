package site.memberservice.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.common.entity.BaseEntity;
import site.memberservice.member.exception.MemberException;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "member",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_nickname",
            columnNames = "nickname"
        ),
        @UniqueConstraint(
            name = "uk_member_phone_number",
            columnNames = "phone_number"
        )
    }
)
@Entity
public class Member extends BaseEntity {

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,16}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @ToString.Exclude
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Embedded
    private PhoneNumber phoneNumber;

    @Embedded
    private Address address;

    private Member(
        final Long id,
        final Email email,
        final String password,
        final String nickname,
        final String name,
        final PhoneNumber phoneNumber,
        final Address address
    ) {
        validateEmail(email);
        validatePassword(password);
        validateNickname(nickname);
        validateName(name);
        validatePhoneNumber(phoneNumber);
        validateAddress(address);

        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public static boolean isValidPassword(final String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static Member create(
        final Email email,
        final String password,
        final String nickname,
        final String name,
        final PhoneNumber phoneNumber,
        final Address address
    ) {
        return new Member(
            null,
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        );
    }

    private void validateEmail(final Email email) {
        if (email == null) {
            throw new MemberException(INVALID_MEMBER_INFO, "회원 이메일은 null일 수 없습니다.");
        }
    }

    private void validatePassword(final String password) {
        if (password == null || password.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 비밀번호는 null 혹은 공백일 수 없습니다. input: %s", password));
        }
    }

    private void validateNickname(final String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 닉네임은 null 혹은 공백일 수 없습니다. input: %s", nickname));
        }

        if (nickname.length() < 2 || nickname.length() > 6) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 닉네임은 2 ~ 6 길이의 문자열만 가능합니다. input: %s", nickname));
        }
    }

    private void validateName(final String name) {
        if (name == null || name.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, format("회원 실명은 null 혹은 공백일 수 없습니다. input: %s", name));
        }
    }

    private void validatePhoneNumber(final PhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            throw new MemberException(INVALID_MEMBER_INFO, "회원 전화번호는 null일 수 없습니다.");
        }
    }

    private void validateAddress(final Address address) {
        if (address == null) {
            throw new MemberException(INVALID_MEMBER_INFO, "회원 주소는 null일 수 없습니다.");
        }
    }
}
