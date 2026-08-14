package site.explorationservice.productindex.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 로컬에서 상품 인덱스가 없으면 매핑과 함께 만들어 둔다.
 * <p>
 * <b>이게 없으면 조용히 깨진다.</b> 인덱스가 없는 상태로 문서를 저장하면 ES가 dynamic mapping으로 인덱스를
 * 자동 생성하는데, 그러면 contentVector가 dense_vector가 아니라 단순 float 배열로 잡힌다. 색인은 성공하지만 kNN 검색이 안 되고, 알아채는
 * 시점에는 이미 재색인 말고는 고칠 방법이 없다.
 * <p>
 * <b>이미 있으면 손대지 않는다.</b> 매핑을 맞춰주지도 고치지도 않는다 — 기존 필드의 analyzer·dims·similarity는
 * 어차피 덮어쓸 수 없고, 앱이 스키마를 바꾸기 시작하면 ddl-auto를 none으로 두기로 한 원칙과 어긋난다.
 * <p>
 * 로컬 프로파일에만 두는 것도 같은 이유다. <b>운영에서 인덱스를 누가 만들지는 아직 정하지 않았다</b> — 배포 절차의 일부로 만들지, 별도 마이그레이션 도구를 둘지는
 * 결정이 필요하다. 그때까지 운영 환경은 인덱스가 미리 준비돼 있어야 한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ProductIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(final ApplicationArguments args) {
        final IndexOperations indexOperations =
            elasticsearchOperations.indexOps(ProductDocument.class);
        final String indexName = indexOperations.getIndexCoordinates().getIndexName();

        if (indexOperations.exists()) {
            log.info("상품 인덱스가 이미 있어 그대로 사용합니다 — {}", indexName);
            return;
        }

        indexOperations.createWithMapping();
        log.info("상품 인덱스를 생성했습니다 — {}", indexName);
    }
}
