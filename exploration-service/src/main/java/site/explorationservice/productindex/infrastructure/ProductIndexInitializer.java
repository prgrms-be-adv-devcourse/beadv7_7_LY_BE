package site.explorationservice.productindex.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 상품 인덱스를 가리키는 별칭이 없으면 매핑과 함께 첫 버전 인덱스를 만들어 별칭을 붙이고, 이미 있으면 매핑에 새로 추가된 필드만 반영한다.
 * <p>
 * <b>인덱스가 없으면 조용히 깨진다.</b> 인덱스가 없는 상태로 문서를 저장하면 ES가 dynamic mapping으로 인덱스를
 * 자동 생성하는데, 그러면 contentVector가 dense_vector가 아니라 단순 float 배열로 잡힌다. 색인은 성공하지만 kNN 검색이 안 되고, 알아채는
 * 시점에는 이미 재색인 말고는 고칠 방법이 없다.
 * <p>
 * <b>이미 있으면 put-mapping으로 병합을 시도한다(교체가 아니다).</b> ES의 put-mapping API는 새 필드만 추가할 수 있고, 기존 필드의
 * analyzer·dims·similarity를 바꾸려 하면 <b>요청 자체가 실패한다.</b> 여기서 주의할 게 있다 — 거부가 곧 "무시하고 넘어감"이 아니다.
 * 실패는 예외로 올라오고, 이 클래스는 {@link ApplicationRunner}라 그 예외가 그대로 애플리케이션 기동을 중단시킨다. 그래서 아래에서 실패를
 * 잡아 경고만 남기고 기동은 계속하게 두었다. 인덱스를 지우거나 다시 만드는 건 사람이 판단할 일이라 여기서 하지 않는다.
 * <p>
 * 실패하는 경우는 크게 둘이다. (1) 기존 필드의 분석기를 바꾼 경우, (2) 새 필드가 기존 인덱스의 settings에 없는 분석기를 요구하는 경우 —
 * settings의 analysis 블록은 열린 인덱스에서 갱신할 수 없고 {@code @Setting} 파일은 인덱스를 만들 때만 적용되기 때문이다. 둘 다
 * 인덱스를 지우고 다시 만든 뒤 재색인해야 해결된다.
 * <p>
 * MySQL에 {@code ddl-auto: none}을 쓰는 이유(같은 스키마를 core-service·member-service 두 앱이 공유해서 생기는 교차 서비스 사고)는
 * 이 인덱스엔 해당하지 않는다 — 검색·추천이 한 모듈로 합쳐지면서 이 인덱스를 쓰는 앱은 exploration-service 하나뿐이다.
 * <p>
 * 로컬 프로파일에만 두지만 운영 인덱스도 이 코드가 만든다 — 백필을 돌릴 때 로컬 프로파일 앱을 운영 클러스터에 붙이기 때문이다. 매핑을 손으로 옮기는 단계가 없어
 * 두 환경의 매핑이 어긋날 경로가 없다. 두 번째 버전부터는 사람이 만든다. 아직 채우지 않은 인덱스에 별칭을 붙이면 안 되는데, 이 클래스는 그 구분을 하지 못하기 때문이다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ProductIndexInitializer implements ApplicationRunner {

    private static final String ALIAS = "lp_products";
    private static final String FIRST_INDEX = "lp_products_v1";

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(final ApplicationArguments args) {
        final IndexOperations entityOperations =
            elasticsearchOperations.indexOps(ProductDocument.class);

        if (!entityOperations.getAliases(ALIAS).isEmpty()) {
            updateMapping(entityOperations, ALIAS);
            return;
        }

        createFirstIndex(entityOperations);
    }

    /**
     * 인덱스가 아니라 <b>별칭</b>이 있는지로 판정한다. 인덱스 존재로 판정하면 다음 버전으로 넘어간 뒤에 앱을 띄웠을 때 아무도 쓰지 않는 첫 버전 인덱스가 다시
     * 만들어진다.
     */
    private void createFirstIndex(final IndexOperations entityOperations) {
        final Settings settings = entityOperations.createSettings(ProductDocument.class);
        final Document mapping = entityOperations.createMapping(ProductDocument.class);

        final IndexOperations firstIndex =
            elasticsearchOperations.indexOps(IndexCoordinates.of(FIRST_INDEX));
        firstIndex.create(settings, mapping);
        firstIndex.alias(new AliasActions().add(new AliasAction.Add(
            AliasActionParameters.builder().withIndices(FIRST_INDEX).withAliases(ALIAS).build())));

        log.info("상품 인덱스를 생성하고 별칭을 붙였습니다 — {} -> {}", ALIAS, FIRST_INDEX);
    }

    /**
     * 매핑 갱신 실패로 애플리케이션이 못 뜨는 일이 없게 예외를 여기서 막는다. 기동을 막아도 사람이 할 일은 똑같은데
     * (인덱스를 지우고 다시 만들기) 서비스 전체가 죽는 대가만 더 든다.
     */
    private void updateMapping(final IndexOperations indexOperations, final String indexName) {
        try {
            indexOperations.putMapping();
            log.info("상품 인덱스가 이미 있어 매핑에 새 필드만 반영했습니다 — {}", indexName);
        } catch (final Exception e) {
            log.warn("상품 인덱스 매핑 갱신에 실패했습니다 — {}. 이번 변경은 기존 필드의 분석기를 바꾸고 기존 인덱스에 없는 분석기를 요구하므로,"
                + " 열려 있는 인덱스에는 반영할 수 없습니다. 인덱스를 지우고 다시 만든 뒤 재색인해야 검색이 정상 동작합니다."
                + " 기동은 계속하지만 이 인덱스의 검색 결과는 옛 매핑 기준입니다", indexName, e);
        }
    }
}
