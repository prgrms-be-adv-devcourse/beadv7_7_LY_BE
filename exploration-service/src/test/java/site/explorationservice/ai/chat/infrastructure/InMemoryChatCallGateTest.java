package site.explorationservice.ai.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("인메모리 챗 호출 게이트")
class InMemoryChatCallGateTest {

    private final InMemoryChatCallGate sut = new InMemoryChatCallGate();

    @Test
    @DisplayName("기본값은 열림 상태다")
    void 기본_열림() {
        assertThat(sut.isOpen()).isTrue();
    }

    @Test
    @DisplayName("close하면 닫히고, open하면 다시 열린다")
    void close_open() {
        sut.close();
        assertThat(sut.isOpen()).isFalse();

        sut.open();
        assertThat(sut.isOpen()).isTrue();
    }
}
