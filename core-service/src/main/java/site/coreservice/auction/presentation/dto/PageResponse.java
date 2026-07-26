package site.coreservice.auction.presentation.dto;

import site.coreservice.auction.application.dto.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        int page,
        int size,
        long totalElements,
        boolean hasNext,
        List<T> items
) {
    public static <R, T> PageResponse<T> from(PageResult<R> result, Function<R, T> itemMapper) {
        return new PageResponse<>(
                result.page(), result.size(), result.totalElements(), result.hasNext(),
                result.items().stream().map(itemMapper).toList()
        );
    }
}
