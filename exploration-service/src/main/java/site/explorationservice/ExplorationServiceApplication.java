package site.explorationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "site.explorationservice",
    "site.common"
})
public class ExplorationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExplorationServiceApplication.class, args);
    }
}
