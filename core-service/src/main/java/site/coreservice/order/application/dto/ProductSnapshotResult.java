package site.coreservice.order.application.dto;

import site.coreservice.order.domain.OrderItemSnapshot;

public record ProductSnapshotResult(
        String albumTitle,
        String artistName,
        Integer releaseYear,
        String pressType,
        String conditionGrade,
        String coverImage
) {

    public static ProductSnapshotResult from(OrderItemSnapshot snapshot) {
        return new ProductSnapshotResult(
                snapshot.getAlbumTitle(),
                snapshot.getArtistName(),
                snapshot.getReleaseYear(),
                snapshot.getPressType(),
                snapshot.getConditionGrade(),
                snapshot.getRepresentativeImageUrl()
        );
    }
}
