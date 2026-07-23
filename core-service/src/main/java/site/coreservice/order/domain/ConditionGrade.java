package site.coreservice.order.domain;

import lombok.Getter;

@Getter
public enum ConditionGrade {

    MINT("새 제품"),
    NEAR_MINT("거의 새것"),
    VERY_GOOD_PLUS("약간의 사용감"),
    VERY_GOOD("사용감 있음"),
    GOOD("많은 사용감"),
    POOR("심한 손상");

    private final String description;

    ConditionGrade(String description) {
        this.description = description;
    }
}
