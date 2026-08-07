package site.pointwalletservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "site.common",
        "site.pointwalletservice"
})
public class PointwalletServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PointwalletServiceApplication.class, args);
    }
}