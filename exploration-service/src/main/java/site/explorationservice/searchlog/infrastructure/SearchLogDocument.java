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
import site.explorationservice.searchlog.domain.SearchLog;

/**
 * 검색 한 건의 기록을 담는 문서.
 * <p>
 * 문서 식별자는 검색 엔진이 채번한다. 같은 검색어를 두 번 치면 기록도 두 건이 남는 것이 맞다.
 * <p>
 * 검색어를 분석하지 않는 타입으로 둔다. 이 필드로 할 일은 "어떤 검색어가 몇 번 들어왔나"를 세는 것이라
 * 입력한 글자 그대로 묶여야 한다. 형태소로 쪼개면 단어별 빈도는 나오지만 검색어 자체를 셀 수 없다.
 * <p>
 * 매핑에 없는 필드가 든 문서는 거부한다. 타입이 잘못 추측되어 굳는 것보다 저장이 실패하는 편이 낫다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "search_logs", dynamic = Dynamic.STRICT)
public class SearchLogDocument {

    /**
     * 검색 엔진이 채번한 값이 그대로 문서 본문에도 실리므로 매핑에 있어야 한다. 매핑에 없는 필드를 거부하도록
     * 설정해 두었기 때문에, 빼두면 모든 저장이 실패한다.
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String searchId;

    @Field(type = FieldType.Keyword)
    private String keyword;

    @Field(type = FieldType.Keyword)
    private String normalizedKeyword;

    @Field(type = FieldType.Keyword)
    private String searchBy;

    @Field(type = FieldType.Integer)
    private int page;

    @Field(type = FieldType.Integer)
    private int size;

    @Field(type = FieldType.Long)
    private long resultCount;

    @Field(type = FieldType.Long)
    private long engineMillis;

    @Field(type = FieldType.Long)
    private long elapsedMillis;

    @Field(type = FieldType.Date)
    private Instant searchedAt;

    public static SearchLogDocument from(final SearchLog searchLog) {
        return SearchLogDocument.builder()
                .searchId(searchLog.searchId())
                .keyword(searchLog.keyword())
                .normalizedKeyword(searchLog.normalizedKeyword())
                .searchBy(searchLog.searchBy())
                .page(searchLog.page())
                .size(searchLog.size())
                .resultCount(searchLog.resultCount())
                .engineMillis(searchLog.engineMillis())
                .elapsedMillis(searchLog.elapsedMillis())
                .searchedAt(searchLog.searchedAt())
                .build();
    }
}
