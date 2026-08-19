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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.common.entity.BaseEntity;
import site.memberservice.member.exception.MemberException;

import java.time.LocalDateTime;
import java.util.Map;

import static java.lang.String.format;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_VIOLATION_HISTORY_REQUEST;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_violation_history")
@Entity
public class MemberViolationHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "violation_type", length = 50, nullable = false)
    private ViolationType violationType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json", nullable = false)
    private Map<String, Object> details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public MemberViolationHistory(
        final Long id,
        final ViolationType violationType,
        final LocalDateTime occurredAt,
        final Map<String, Object> details,
        final Member member
    ) {
        validateViolationType(violationType);
        validateOccurredAt(occurredAt);
        validateDetails(details);
        validateMember(member);

        this.id = id;
        this.violationType = violationType;
        this.occurredAt = occurredAt;
        this.details = details;
        this.member = member;
    }

    public static MemberViolationHistory create(
        final ViolationType violationType,
        final LocalDateTime occurredAt,
        final Map<String, Object> details,
        final Member member
    ) {
        return new MemberViolationHistory(
            null,
            violationType,
            occurredAt,
            details,
            member
        );
    }

    private void validateViolationType(final ViolationType violationType) {
        if (violationType == null) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, "위반 유형은 null일 수 없습니다.");
        }
    }

    private void validateOccurredAt(final LocalDateTime occurredAt) {
        if (occurredAt == null) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, "위반 발생일시는 null일 수 없습니다.");
        }
    }

    private void validateDetails(final Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, format("위반 상세 내용은 null 혹은 빈 값일 수 없습니다. input: %s", details));
        }
    }

    private void validateMember(final Member member) {
        if (member == null) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, "회원 정보는 null일 수 없습니다.");
        }
    }
}
