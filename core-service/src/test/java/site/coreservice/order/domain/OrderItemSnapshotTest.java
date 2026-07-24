package site.coreservice.order.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderItemSnapshot")
class OrderItemSnapshotTest {

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("모든 필드 값이 같으면 equals는 true를 반환한다")
        void sameValuesAreEqual() {
            // given
            OrderItemSnapshot a = OrderItemSnapshot.of("Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    ConditionGrade.VERY_GOOD_PLUS, "https://cdn.example.com/listings/5001/photo1.jpg");
            OrderItemSnapshot b = OrderItemSnapshot.of("Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    ConditionGrade.VERY_GOOD_PLUS, "https://cdn.example.com/listings/5001/photo1.jpg");

            // when & then
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("필드 값이 하나라도 다르면 equals는 false를 반환한다")
        void differentValuesAreNotEqual() {
            // given
            OrderItemSnapshot a = OrderItemSnapshot.of("Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    ConditionGrade.VERY_GOOD_PLUS, "https://cdn.example.com/listings/5001/photo1.jpg");
            OrderItemSnapshot b = OrderItemSnapshot.of("Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    ConditionGrade.GOOD, "https://cdn.example.com/listings/5001/photo1.jpg");

            // when & then
            assertThat(a).isNotEqualTo(b);
        }
    }
}
