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

import java.time.LocalDateTime;

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
        this.id = id;
        this.restrictionType = restrictionType;
        this.reason = reason;
        this.restrictedAt = restrictedAt;
        this.restrictedUntil = restrictedUntil;
        this.member = member;
    }
}
