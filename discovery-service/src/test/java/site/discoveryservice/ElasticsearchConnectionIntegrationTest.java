package site.discoveryservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

/**
 * 애플리케이션과 Elasticsearch 사이의 배선을 실 서버로 검증한다.
 * 실행 전제: docker/local의 elasticsearch 컨테이너 기동(localhost:9200).
 * <p>
 * actuator health만으로는 부족해서 둔 테스트다. health는 ES를 내리면 DOWN으로 바뀌므로 "연결됐다"까지는
 * 증명하지만, 응답이 워낙 단순해서 JsonpMapper가 Jackson 2든 3든 통과한다 — 프로젝트 전체가 Jackson 3인데
 * ES 클라이언트의 기본 매퍼는 Jackson 2라, 그 불일치가 드러나려면 실제 도큐먼트를 색인하고 되읽어서
 * 직렬화·역직렬화를 온전히 태워야 한다. (Kafka 전환 때 JsonSerializer가 Jackson 2 전제라 터졌던 것과 같은 계열)
 * <p>
 * 조회는 검색이 아니라 id 기준 get으로 한다. ES는 색인 직후 바로 검색되지 않고 refresh(기본 1초)를 거쳐야
 * 하는데, get API는 그 주기와 무관하게 즉시 읽히기 때문이다. 여기서 보려는 건 검색 동작이 아니라 배선이라
 * 굳이 refresh를 기다리거나 강제하지 않는다.
 * <p>
 * 인덱스는 실제 상품 인덱스가 아닌 이 테스트 전용 이름을 쓰고, 매 테스트 후 통째로 지운다. 도큐먼트를
 * 하나씩 지우는 것보다 확실하고, 전용 이름이라 실제 데이터와 섞이지 않는다. 도큐먼트 스키마도 이 안에만
 * 두는데, 실제 lp_products 매핑 설계는 별도 작업이라 여기서 형태를 굳혀버리지 않기 위해서다.
 * <p>
 * 정리를 @AfterAll이 아니라 @AfterEach로 둔 이유는 주입받은 ElasticsearchOperations를 쓰기 때문이다
 * (@AfterAll은 static이라 인스턴스 필드에 접근할 수 없다). 테스트가 예외로 중단되면 인덱스가 남을 수
 * 있는데, 전용 이름이라 수동으로 지워도 실제 데이터에 영향이 없다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("Elasticsearch 연결")
class ElasticsearchConnectionIntegrationTest {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @AfterEach
    void tearDown() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ConnectionCheckDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
    }

    @Test
    @DisplayName("도큐먼트를 색인하고 id로 다시 읽으면 모든 필드가 그대로 보존된다")
    void savesAndReadsBackDocument() {
        // given
        String id = UUID.randomUUID().toString();
        LocalDateTime indexedAt = LocalDateTime.of(2026, 8, 7, 15, 30, 45);
        ConnectionCheckDocument document =
                new ConnectionCheckDocument(id, "Kind of Blue", 1959, indexedAt);

        // when
        elasticsearchOperations.save(document);
        ConnectionCheckDocument found = elasticsearchOperations.get(id, ConnectionCheckDocument.class);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getTitle()).isEqualTo("Kind of Blue");
        assertThat(found.getReleaseYear()).isEqualTo(1959);
        assertThat(found.getIndexedAt()).isEqualTo(indexedAt);
    }

    @Document(indexName = "discovery-connection-test")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ConnectionCheckDocument {

        @Id
        private String id;

        @Field(type = FieldType.Text)
        private String title;

        @Field(type = FieldType.Integer)
        private int releaseYear;

        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
        private LocalDateTime indexedAt;
    }
}
