package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.ArtistRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ArtistRepositoryImpl implements ArtistRepository {

    private final ArtistJpaRepository artistJpaRepository;

    @Override
    public Artist save(Artist artist) {
        return artistJpaRepository.save(artist);
    }

    @Override
    public Optional<Artist> findByNormalizedName(String normalizedName) {
        return artistJpaRepository.findByNormalizedName(normalizedName);
    }
}
