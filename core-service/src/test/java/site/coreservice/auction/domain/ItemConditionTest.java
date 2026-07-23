package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.auction.exception.InvalidValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemConditionTest {

    @Test
    @DisplayName("유효한 이름이면 해당 ItemCondition을 반환한다")
    void testFrom_validValue_returnsEnum() {
        // when
        ItemCondition condition = ItemCondition.from("MINT");

        // then
        assertThat(condition).isEqualTo(ItemCondition.MINT);
    }

    @Test
    @DisplayName("유효하지 않은 값이면 InvalidValueException을 던진다")
    void testFrom_invalidValue_throws() {
        // when & then
        assertThatThrownBy(() -> ItemCondition.from("NOT_A_CONDITION")).isInstanceOf(InvalidValueException.class);
    }
}
