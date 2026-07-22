package site.coreservice.product.application;

import java.util.List;
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;

/** 카탈로그 상세 조회 결과(명세 1-3). openAuctionCount는 경매 count API 연동(D7~) 때 추가한다. */
public record ProductDetailResult(
        Long productId,
        String catalogNumber,
        String title,
        ArtistResult artist,
        String label,
        String country,
        int releaseYear,
        PressType pressType,
        String format,
        String genre,
        String coverImageUrl,
        String description
) {
    public static ProductDetailResult of(Product product, Artist artist) {
        return new ProductDetailResult(
                product.getId(),
                product.getCatalogNumber(),
                product.getTitle(),
                new ArtistResult(artist.getId(), artist.getName(), List.copyOf(artist.getAliases())),
                product.getLabel(),
                product.getReleaseCountry(),
                product.getReleaseYear(),
                product.getPressType(),
                product.getFormat(),
                product.getGenre(),
                product.getCoverImage(),
                product.getDescription()
        );
    }

    public record ArtistResult(Long artistId, String name, List<String> aliases) {
    }
}
