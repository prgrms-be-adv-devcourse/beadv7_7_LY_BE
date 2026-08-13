package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import site.memberservice.member.exception.MemberException;
import site.memberservice.util.NullAndBlankSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberRestriction Entity")
class MemberRestrictionTest {

    @DisplayName("유효한 회원 제재 정보를 입력하면 id가 null인 MemberRestriction 객체가 생성된다.")
    @Test
    void createMemberRestriction() {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final String reason = "낙찰 후 미결제";
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final LocalDateTime restrictedUntil = LocalDateTime.of(2026, 8, 20, 0, 0);
        final Member member = createMember();

        // When
        final MemberRestriction memberRestriction = MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        );

        // Then
        assertThat(memberRestriction.getId()).isNull();
    }

    @DisplayName("제재 유형으로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputRestrictionTypeNull() {
        // Given
        final RestrictionType restrictionType = null;
        final String reason = "낙찰 후 미결제";
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final LocalDateTime restrictedUntil = LocalDateTime.of(2026, 8, 20, 0, 0);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("제재 유형은 null일 수 없습니다.");
    }

    @DisplayName("제재 사유로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputReasonNullOrBlank(final String reason) {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final LocalDateTime restrictedUntil = LocalDateTime.of(2026, 8, 20, 0, 0);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("제재 사유는 null 혹은 공백일 수 없습니다. input: %s", reason));
    }

    @DisplayName("제재 시작일시로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputRestrictedAtNull() {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final String reason = "낙찰 후 미결제";
        final LocalDateTime restrictedAt = null;
        final LocalDateTime restrictedUntil = LocalDateTime.of(2026, 8, 20, 0, 0);
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("제재 시작일시는 null일 수 없습니다.");
    }

    @DisplayName("제재 종료일시로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputRestrictedUntilNull() {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final String reason = "낙찰 후 미결제";
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final LocalDateTime restrictedUntil = null;
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("제재 종료일시는 null일 수 없습니다.");
    }

    @DisplayName("제재 종료일시가 제재 시작일시 이후가 아니면 예외가 발생한다.")
    @MethodSource("invalidRestrictionPeriods")
    @ParameterizedTest
    void throwExceptionWhenRestrictedUntilIsNotAfterRestrictedAt(
        final LocalDateTime restrictedAt,
        final LocalDateTime restrictedUntil
    ) {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final String reason = "낙찰 후 미결제";
        final Member member = createMember();

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format(
                "제재 종료일시는 제재 시작일시 이후여야 합니다. restrictedAt: %s, restrictedUntil: %s",
                restrictedAt,
                restrictedUntil
            ));
    }

    private static Stream<Arguments> invalidRestrictionPeriods() {
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);

        return Stream.of(
            Arguments.of(restrictedAt, restrictedAt),                  // 1. 시작일시와 종료일시가 동일한 경우
            Arguments.of(restrictedAt, restrictedAt.minusDays(1))      // 2. 종료일시가 시작일시보다 이전인 경우
        );
    }

    @DisplayName("회원 객체로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputMemberNull() {
        // Given
        final RestrictionType restrictionType = RestrictionType.AUCTION_BIDDING;
        final String reason = "낙찰 후 미결제";
        final LocalDateTime restrictedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        final LocalDateTime restrictedUntil = LocalDateTime.of(2026, 8, 20, 0, 0);
        final Member member = null;

        // When & Then
        assertThatThrownBy(() -> MemberRestriction.create(
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
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
