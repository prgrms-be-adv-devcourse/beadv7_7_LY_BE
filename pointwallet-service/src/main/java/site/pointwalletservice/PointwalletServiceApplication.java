package site.pointwalletservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.resilience.annotation.EnableResilientMethods;
import site.pointwalletservice.deposit.infrastructure.toss.TossPaymentsProperties;

// 스프링 부트 4 / 스프링 프레임워크 7부터 재시도 기능이 spring-core에 내장돼서 별도 의존성(spring-retry)
// 없이 쓸 수 있다 - @EnableRetry(구 spring-retry)가 아니라 @EnableResilientMethods(신 core 기능)를 쓴다.
@EnableResilientMethods
@SpringBootApplication(scanBasePackages = {
        "site.common",
        "site.pointwalletservice"
})
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class PointwalletServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PointwalletServiceApplication.class, args);
    }
}