package site.productservice.domain;

import java.util.List;
import java.util.Optional;

/**
 * 아티스트 저장소 (도메인 인터페이스).
 * 구현체는 infrastructure의 ArtistRepositoryImpl.
 */
public interface ArtistRepository {

    Artist save(Artist artist);

    Optional<Artist> findByNormalizedName(String normalizedName);

    Optional<Artist> findById(Long id);

    List<Artist> findAllByIds(List<Long> ids);
}
