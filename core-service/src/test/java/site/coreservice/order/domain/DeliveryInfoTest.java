package site.coreservice.order.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DeliveryInfo VO")
class DeliveryInfoTest {

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("모든 필드 값이 같으면 equals는 true를 반환한다")
        void sameValuesAreEqual() {
            // given
            DeliveryInfo a = DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");
            DeliveryInfo b = DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");

            // when & then
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("필드 값이 하나라도 다르면 equals는 false를 반환한다")
        void differentValuesAreNotEqual() {
            // given
            DeliveryInfo a = DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");
            DeliveryInfo b = DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "102동 303호");

            // when & then
            assertThat(a).isNotEqualTo(b);
        }
    }
}
