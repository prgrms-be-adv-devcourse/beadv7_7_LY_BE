package site.coreservice.order.application.port;

public record ProductInfo(
        String title,
        String artistName,
        Integer releaseYear,
        String pressType
) {
}
