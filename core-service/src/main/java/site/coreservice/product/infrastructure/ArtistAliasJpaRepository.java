package site.coreservice.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.ArtistAlias;

public interface ArtistAliasJpaRepository extends JpaRepository<ArtistAlias, Long> {

    boolean existsByArtistIdAndNormalizedName(Long artistId, String normalizedName);
}
