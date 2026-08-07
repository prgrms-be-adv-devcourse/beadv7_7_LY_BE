package site.auctionservice.application.port.dto;

public record ProductDetail(
        Long productId,
        String title,
        String artistName,
        String coverImageUrl,
        Integer releaseYear,
        String genre,
        String pressType,
        boolean active
) {
}
