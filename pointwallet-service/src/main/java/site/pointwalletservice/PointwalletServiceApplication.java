package site.pointwalletservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import site.pointwalletservice.deposit.infrastructure.toss.TossPaymentsProperties;

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