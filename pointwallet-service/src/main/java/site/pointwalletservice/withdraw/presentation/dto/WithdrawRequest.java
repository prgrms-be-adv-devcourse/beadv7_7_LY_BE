package site.pointwalletservice.withdraw.presentation.dto;
import java.math.BigDecimal;

// 원래는 Idempotency-Key 헤더로 받았으나, 프론트-게이트웨이가 다른 오리진으로 배포되면서
// 게이트웨이 CORS 허용 헤더 목록에 없어 preflight가 막혔다(다른 표준 헤더는 이미 허용돼 있었음).
// 게이트웨이 쪽 CORS 설정 수정을 기다리지 않고 pointwallet-service 자체 변경만으로 풀기 위해
// 바디 필드로 옮긴다.
public record WithdrawRequest(BigDecimal amount, String idempotencyKey) {
}