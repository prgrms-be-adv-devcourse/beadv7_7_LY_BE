package site.auctionservice.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResult<T>(int page, int size, long totalElements, boolean hasNext, List<T> items) {
    public static <T> PageResult<T> of(Page<?> springPage, List<T> items) {
        return new PageResult<>(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.hasNext(),
                items
        );
    }
}
