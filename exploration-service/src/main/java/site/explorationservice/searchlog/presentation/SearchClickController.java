package site.explorationservice.searchlog.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.searchlog.application.SearchLogService;
import site.explorationservice.searchlog.presentation.dto.SearchClickRequest;

/**
 * 검색 결과 클릭을 기록한다.
 * <p>
 * 값 검증은 요청을 기록으로 바꾸는 자리에서 끝난다. 저장은 다른 스레드로 넘어가기 때문에, 그 안에서
 * 검증하면 잘못된 요청에도 성공 응답이 나가고 부른 쪽이 무엇이 잘못됐는지 알 수 없다.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchClickController {

    private final SearchLogService searchLogService;

    @PostMapping("/clicks")
    public ApiResponse<Void> saveSearchClick(@RequestBody final SearchClickRequest request) {
        searchLogService.saveClickLog(request.toSearchClickLog());
        return ApiResponse.success();
    }
}
