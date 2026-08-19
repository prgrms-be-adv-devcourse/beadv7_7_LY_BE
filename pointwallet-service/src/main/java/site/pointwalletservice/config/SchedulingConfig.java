// config/SchedulingConfig.java — auction-service/fulfillment-service와 동일한 패턴, pointwallet-service엔 아직 없어서 신규 추가
package site.pointwalletservice.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}