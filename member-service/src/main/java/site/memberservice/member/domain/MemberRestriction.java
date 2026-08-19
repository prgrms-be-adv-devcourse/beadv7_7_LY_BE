package site.memberservice.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.memberservice.member.exception.MemberException;

import java.time.LocalDateTime;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_RESTRICTION_REQUEST;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_restriction")
@Entity
public class MemberRestriction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "restriction_type", length = 50, nullable = false)
    private RestrictionType restrictionType;

    @Column(name = "reason", length = 100, nullable = false)
    private String reason;

    @Column(name = "restricted_at", nullable = false)
    private LocalDateTime restrictedAt;

    @Column(name = "restricted_until", nullable = false)
    private LocalDateTime restrictedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public MemberRestriction(
        final Long id,
        final RestrictionType restrictionType,
        final String reason,
        final LocalDateTime restrictedAt,
        final LocalDateTime restrictedUntil,
        final Member member
    ) {
        validateRestrictionType(restrictionType);
        validateReason(reason);
        validateRestrictionPeriod(restrictedAt, restrictedUntil);
        validateMember(member);

        this.id = id;
        this.restrictionType = restrictionType;
        this.reason = reason;
        this.restrictedAt = restrictedAt;
        this.restrictedUntil = restrictedUntil;
        this.member = member;
    }

    public static MemberRestriction create(
        final RestrictionType restrictionType,
        final String reason,
        final LocalDateTime restrictedAt,
        final LocalDateTime restrictedUntil,
        final Member member
    ) {
        return new MemberRestriction(
            null,
            restrictionType,
            reason,
            restrictedAt,
            restrictedUntil,
            member
        );
    }

    private void validateRestrictionType(final RestrictionType restrictionType) {
        if (restrictionType == null) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, "제재 유형은 null일 수 없습니다.");
        }
    }

    private void validateReason(final String reason) {
        if (reason == null || reason.isBlank()) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, format("제재 사유는 null 혹은 공백일 수 없습니다. input: %s", reason));
        }
    }

    private void validateRestrictionPeriod(final LocalDateTime restrictedAt, final LocalDateTime restrictedUntil) {
        if (restrictedAt == null) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, "제재 시작일시는 null일 수 없습니다.");
        }

        if (restrictedUntil == null) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, "제재 종료일시는 null일 수 없습니다.");
        }

        if (!restrictedUntil.isAfter(restrictedAt)) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, format(
                "제재 종료일시는 제재 시작일시 이후여야 합니다. restrictedAt: %s, restrictedUntil: %s",
                restrictedAt,
                restrictedUntil
            ));
        }
    }

    private void validateMember(final Member member) {
        if (member == null) {
            throw new MemberException(INVALID_RESTRICTION_REQUEST, "회원 정보는 null일 수 없습니다.");
        }
    }
}
