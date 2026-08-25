package site.explorationservice.ai.chat.domain;

/**
 * 챗 모델(OpenAI) 호출을 켜고 끄는 스위치 — 장애가 길어질 때 운영자가 닫아서 더 이상 호출을 시도하지 않게 할 수 있다.
 * <p>
 * 현재는 인메모리 구현체 뿐으로, 스케일 아웃 시 Redis로 전환 필요
 */
public interface ChatCallGate {

    boolean isOpen();

    void open();

    void close();
}
