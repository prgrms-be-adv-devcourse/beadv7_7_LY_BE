package site.productservice.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Override
    public Map<Long, List<String>> findNamesByArtistIds(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistAliasJpaRepository.findByArtistIdIn(artistIds).stream()
                .collect(Collectors.groupingBy(ArtistAlias::getArtistId,
                        Collectors.mapping(ArtistAlias::getName, Collectors.toList())));
    }
}
