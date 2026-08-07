package site.productservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.productservice.domain.ArtistAlias;
import site.productservice.domain.ArtistAliasRepository;

@Repository
@RequiredArgsConstructor
public class ArtistAliasRepositoryImpl implements ArtistAliasRepository {

    private final ArtistAliasJpaRepository artistAliasJpaRepository;

    @Override
    public ArtistAlias save(ArtistAlias alias) {
        return artistAliasJpaRepository.save(alias);
    }

    @Override
    public boolean hasAlias(Long artistId, String normalizedName) {
        return artistAliasJpaRepository.existsByArtistIdAndNormalizedName(artistId, normalizedName);
    }
}
