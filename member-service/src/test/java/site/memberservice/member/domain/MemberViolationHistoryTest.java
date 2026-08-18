package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import site.memberservice.member.exception.MemberException;

import java.time.LocalDateTime;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberViolationHistory Entity")
class MemberViolationHistoryTest {

    @DisplayName("유효한 회원 위반 이력 정보를 입력하면 id가 null인 MemberViolationHistory 객체가 생성된다.")
    @Test
    void createMemberViolationHistory() {
        // Given
        final ViolationType violationType = ViolationType.ORDER_CANCELLED;
        final LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final Map<String, Object> details = Map.of("orderId", 1, "auctionId", 2);
        final Member member = createMember();

        // When
        final MemberViolationHistory memberViolationHistory = MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        );

        // Then
        assertThat(memberViolationHistory.getId()).isNull();
    }

    @DisplayName("위반 유형으로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputViolationTypeNull() {
        // Given
        final ViolationType violationType = null;
        final LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final Map<String, Object> details = Map.of("orderId", 1, "auctionId", 2);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("위반 유형은 null일 수 없습니다.");
    }

    @DisplayName("위반 발생일시로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputOccurredAtNull() {
        // Given
        final ViolationType violationType = ViolationType.ORDER_CANCELLED;
        final LocalDateTime occurredAt = null;
        final Map<String, Object> details = Map.of("orderId", 1, "auctionId", 2);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("위반 발생일시는 null일 수 없습니다.");
    }

    @DisplayName("위반 상세 내용으로 null 혹은 빈 값을 입력하면 예외가 발생한다.")
    @NullSource
    @ParameterizedTest
    void throwExceptionWhenInputDetailsNullOrEmpty(final Map<String, Object> details) {
        // Given
        final ViolationType violationType = ViolationType.ORDER_CANCELLED;
        final LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("위반 상세 내용은 null 혹은 빈 값일 수 없습니다. input: %s", details));
    }

    @DisplayName("위반 상세 내용으로 빈 Map을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputDetailsEmpty() {
        // Given
        final ViolationType violationType = ViolationType.ORDER_CANCELLED;
        final LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final Map<String, Object> details = Map.of();
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("위반 상세 내용은 null 혹은 빈 값일 수 없습니다. input: %s", details));
    }

    @DisplayName("회원 객체로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputMemberNull() {
        // Given
        final ViolationType violationType = ViolationType.ORDER_CANCELLED;
        final LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final Map<String, Object> details = Map.of("orderId", 1, "auctionId", 2);
        final Member member = null;

        // When & Then
        assertThatThrownBy(() -> MemberViolationHistory.create(
            violationType,
            occurredAt,
            details,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 정보는 null일 수 없습니다.");
    }

    private Member createMember() {
        return new Member(
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
    }
}
