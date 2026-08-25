package site.explorationservice.ai.chat.infrastructure;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.explorationservice.ai.chat.domain.ChatCallGate;

@Slf4j
@Component
public class InMemoryChatCallGate implements ChatCallGate {

    private final AtomicBoolean open = new AtomicBoolean(true);

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void open() {
        open.set(true);
        log.info("챗 모델 호출을 재개합니다");
    }

    @Override
    public void close() {
        open.set(false);
        log.warn("챗 모델 호출을 중지합니다");
    }
}
