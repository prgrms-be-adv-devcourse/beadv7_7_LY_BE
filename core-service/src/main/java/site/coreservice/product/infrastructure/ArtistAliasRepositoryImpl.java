package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.ArtistAlias;
import site.coreservice.product.domain.ArtistAliasRepository;

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
