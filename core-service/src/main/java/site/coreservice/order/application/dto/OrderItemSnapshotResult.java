package site.coreservice.order.application.dto;

import site.coreservice.order.domain.OrderItemSnapshot;

public record OrderItemSnapshotResult(
        Long productId,
        String albumTitle,
        String artistName,
        Integer releaseYear,
        String pressType,
        String conditionGrade,
        String coverImage
) {

    public static OrderItemSnapshotResult from(Long productId, OrderItemSnapshot snapshot) {
        return new OrderItemSnapshotResult(
                productId,
                snapshot.getAlbumTitle(),
                snapshot.getArtistName(),
                snapshot.getReleaseYear(),
                snapshot.getPressType(),
                snapshot.getConditionGrade(),
                snapshot.getRepresentativeImageUrl()
        );
    }
}
