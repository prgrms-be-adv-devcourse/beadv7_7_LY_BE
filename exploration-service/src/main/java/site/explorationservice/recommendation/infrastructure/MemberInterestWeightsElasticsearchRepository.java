package site.explorationservice.recommendation.infrastructure;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface MemberInterestWeightsElasticsearchRepository
    extends ElasticsearchRepository<MemberInterestWeightsDocument, Long> {

}
