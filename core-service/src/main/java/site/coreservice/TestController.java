package site.coreservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.web.MemberId;

@RestController
public class TestController {

    @GetMapping("/api/v1/product/white")
    public String white() {
        return "core-service 인증 필요없는 white 호출 성공!";
    }

    @GetMapping("/api/v1/product/black")
    public String black(@MemberId Long memberId) {
        return "core-service 인증 필요한 black 호출 성공! - " + memberId;
    }
}
