package site.coreservice.product.presentation.dto;

import site.coreservice.product.application.dto.ProductDetailResult;

/** 상세 응답(명세 1-3). 필드명은 api명세 기준(catalogNo·country·coverImageUrl). openAuctionCount는 경매 count 연동(D7~) 때 추가. */
public record ProductDetailResponse(
        Long productId,
        String catalogNo,
        String title,
        ArtistResponse artist,
        String label,
        String country,
        int releaseYear,
        String pressType,
        String format,
        String genre,
        String coverImageUrl,
        String description
) {
    public static ProductDetailResponse from(ProductDetailResult result) {
        return new ProductDetailResponse(
                result.productId(),
                result.catalogNumber(),
                result.title(),
                new ArtistResponse(result.artist().artistId(), result.artist().name()),
                result.label(),
                result.country(),
                result.releaseYear(),
                result.pressType().name(),
                result.format(),
                result.genre(),
                result.coverImageUrl(),
                result.description()
        );
    }

    public record ArtistResponse(Long artistId, String name) {
    }
}
