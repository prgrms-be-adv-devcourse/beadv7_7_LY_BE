package site.fulfillmentservice.order.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DeliveryInfo VO")
class DeliveryInfoTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("수령인이 없으면 예외가 발생한다")
        void throwsWhenRecipientNameMissing() {
            assertThatThrownBy(() -> DeliveryInfo.of(null, "010-1234-5678", "서울시 강남구", "101동 202호"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("연락처가 공백이면 예외가 발생한다")
        void throwsWhenPhoneNumberBlank() {
            assertThatThrownBy(() -> DeliveryInfo.of("홍길동", "   ", "서울시 강남구", "101동 202호"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("기본주소가 없으면 예외가 발생한다")
        void throwsWhenBaseAddressMissing() {
            assertThatThrownBy(() -> DeliveryInfo.of("홍길동", "010-1234-5678", null, "101동 202호"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("상세주소가 없으면 예외가 발생한다")
        void throwsWhenDetailAddressMissing() {
            assertThatThrownBy(() -> DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

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
