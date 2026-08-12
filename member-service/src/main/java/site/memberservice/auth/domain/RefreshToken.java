package site.memberservice.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "refresh_token",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_id",
            columnNames = "member_id"
        )
    }
)
@Entity
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "value", length = 512, nullable = false)
    private String value;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public RefreshToken(final Long id, final String value, final Long memberId) {
        this.id = id;
        this.value = value;
        this.memberId = memberId;
    }

    public static RefreshToken create(final String value, final Long memberId) {
        return new RefreshToken(null, value, memberId);
    }
}
