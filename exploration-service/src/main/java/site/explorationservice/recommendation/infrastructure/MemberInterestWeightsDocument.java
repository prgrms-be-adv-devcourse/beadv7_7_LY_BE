package site.explorationservice.recommendation.infrastructure;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * {@code InterestWeightCacheRepository}의 ES 매핑 문서. 이 클래스는 infrastructure 밖으로 나가지 않는다 — 도메인/애플리케이션
 * 계층은 {@code AxisWeights}만 주고받는다.
 * <p>
 * 문서 id는 memberId다. 갱신 시 마다 같은 memberId로 덮어써서 멱등하다
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "member_interest_weights")
public class MemberInterestWeightsDocument {

    @Id
    private Long memberId;

    @Field(type = FieldType.Double)
    private double identityWeight;

    @Field(type = FieldType.Double)
    private double originWeight;

    @Field(type = FieldType.Double)
    private double editionWeight;

    @Field(type = FieldType.Date)
    private Instant computedAt;
}
