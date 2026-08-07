package site.fulfillmentservice.order.application.port;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(
        String title,
        String artistName,
        Integer releaseYear,
        String pressType
) {
}
