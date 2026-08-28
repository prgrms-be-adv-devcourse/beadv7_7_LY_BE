package site.explorationservice.searchlog.infrastructure;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import site.explorationservice.searchlog.domain.SearchClickLog;

/**
 * 클릭 한 건의 기록을 담는 문서.
 * <p>
 * 검색 기록과 인덱스를 나눈 이유는 클릭이 검색의 일부에서만 일어나기 때문이다. 한 문서에 담으면 클릭이 생길
 * 때마다 이미 저장된 문서를 고쳐야 하는데, 검색 엔진의 문서 수정은 통째로 다시 쓰는 방식이라 비싸다.
 * <p>
 * 두 기록은 검색 식별자로 잇는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "search_click_logs", dynamic = Dynamic.STRICT)
public class SearchClickLogDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String searchId;

    @Field(type = FieldType.Long)
    private Long productId;

    /** 눌린 항목이 결과의 몇 번째였는지. 1부터 세고 페이지를 넘어가도 이어 센다. */
    @Field(type = FieldType.Integer)
    private int rank;

    @Field(type = FieldType.Date)
    private Instant clickedAt;

    public static SearchClickLogDocument from(final SearchClickLog clickLog) {
        return SearchClickLogDocument.builder()
                .searchId(clickLog.searchId())
                .productId(clickLog.productId())
                .rank(clickLog.rank())
                .clickedAt(clickLog.clickedAt())
                .build();
    }
}
