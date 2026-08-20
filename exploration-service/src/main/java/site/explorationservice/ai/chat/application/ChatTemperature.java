package site.explorationservice.ai.chat.application;

import lombok.Getter;

/**
 * 챗 모델 호출 시 쓸 temperature를 의도 단위로 고른다.
 */
@Getter
public enum ChatTemperature {


    // 구조화된 답이 필요한 작업 — 매번 비슷한 판단 기준으로 안정적인 값을 받는 게 목적
    LOW(0.2),

    // 일반적인 대화·질의응답
    MIDDLE(0.6),

    // 창의성·다양성이 필요한 작업
    HIGH(1.0);

    private final double value;

    ChatTemperature(final double value) {
        this.value = value;
    }
}
