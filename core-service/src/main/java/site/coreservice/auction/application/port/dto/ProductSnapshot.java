package site.coreservice.auction.application.port.dto;

public record ProductSnapshot(
        Long productId,
        String title,
        String artistName,
        Integer releaseYear,
        String genre,
        String pressType,
        boolean active
) {
}
