package site.coreservice.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.Artist;

import java.util.Optional;

public interface ArtistJpaRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByNormalizedName(String normalizedName);
}
