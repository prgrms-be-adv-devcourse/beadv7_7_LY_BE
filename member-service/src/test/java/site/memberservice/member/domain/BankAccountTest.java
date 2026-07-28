package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import site.memberservice.member.exception.MemberException;
import site.memberservice.util.NullAndBlankSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BankAccount Entity")
class BankAccountTest {

    // TODO : #102 테스트 데이터 Fixture 분리 꼭 적용
    
    @DisplayName("유효한 은행 계좌 정보를 입력하면 id가 null인 BankAccount 객체가 생성된다.")
    @Test
    void createBankAccount() {
        // Given
        final String accountNumber = "110-123-456789";
        final String bankName = "켈리뱅크";
        final Member member = new Member(
            1L,
            new Email("test@email.com"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        
        // When
        final BankAccount bankAccount = BankAccount.create(accountNumber, bankName, member);

        // Then
        assertThat(bankAccount.getId()).isNull();
    }

    @DisplayName("은행 계좌번호로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputBankAccountNumberNullOrBlank(final String accountNumber) {
        // Given
        final String bankName = "켈리뱅크";
        final Member member = new Member(
            1L,
            new Email("test@email.com"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );

        // When & Then
        assertThatThrownBy(() -> BankAccount.create(accountNumber, bankName, member))
            .isInstanceOf(MemberException.class)
            .hasMessage("은행 계좌 번호는 null 혹은 공백일 수 없습니다.");
    }

    @DisplayName("유효하지 않은 형태의 은행 계좌번호를 입력하면 예외가 발생한다.")
    @ValueSource(strings = {
        "110-abc-456789",   // 1. 숫자가 아닌 문자가 포함된 경우
        "-110123456789",    // 2. 하이픈(-)으로 시작하는 경우
        "110123456789-",    // 3. 하이픈(-)으로 끝나는 경우
        "110--123-456789",  // 4. 하이픈(-)이 연속으로 들어간 경우
    })
    @ParameterizedTest
    void throwExceptionWhenInputInvalidBankAccountNumber(final String accountNumber) {
        // Given
        final String bankName = "켈리뱅크";
        final Member member = new Member(
            1L,
            new Email("test@email.com"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );

        // When & Then
        assertThatThrownBy(() -> BankAccount.create(accountNumber, bankName, member))
            .isInstanceOf(MemberException.class)
            .hasMessage("유효하지 않은 형태의 은행 계좌 번호입니다.");
    }

    @DisplayName("은행명으로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputBankNameNullOrBlank(final String bankName) {
        // Given
        final String accountNumber = "110-123-456789";
        final Member member = new Member(
            1L,
            new Email("test@email.com"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );

        // When & Then
        assertThatThrownBy(() -> BankAccount.create(accountNumber, bankName, member))
            .isInstanceOf(MemberException.class)
            .hasMessage("은행명은 null 혹은 공백일 수 없습니다.");
    }

    @DisplayName("회원 객체로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputMemberNull() {
        // Given
        final String accountNumber = "110-123-456789";
        final String bankName = "켈리뱅크";
        final Member member = null;

        // When & Then
        assertThatThrownBy(() -> BankAccount.create(accountNumber, bankName, member))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 정보는 null일 수 없습니다.");
    }
}
