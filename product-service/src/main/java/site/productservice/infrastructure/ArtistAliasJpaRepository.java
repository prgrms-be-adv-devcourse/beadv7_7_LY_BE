package site.productservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.productservice.domain.ArtistAlias;

public interface ArtistAliasJpaRepository extends JpaRepository<ArtistAlias, Long> {

    boolean existsByArtistIdAndNormalizedName(Long artistId, String normalizedName);
}
