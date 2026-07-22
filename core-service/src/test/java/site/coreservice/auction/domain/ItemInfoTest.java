package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemInfoTest {

    @Test
    @DisplayName("상품 상태가 null이면 예외가 발생한다")
    void testFrom_nullCondition_throws() {
        // when & then
        assertThatThrownBy(() -> ItemInfo.from(null, "충분히 긴 설명입니다.", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("설명이 없으면(null) 길이 검증을 건너뛴다")
    void testFrom_nullDescription_isAllowed() {
        // when
        ItemInfo itemInfo = ItemInfo.from(ItemCondition.MINT, null, null);

        // then
        assertThat(itemInfo.getDescription()).isNull();
    }

    @Test
    @DisplayName("설명이 최소 길이보다 짧으면 예외가 발생한다")
    void testFrom_descriptionTooShort_throws() {
        // given
        String tooShort = "짧음";

        // when & then
        assertThatThrownBy(() -> ItemInfo.from(ItemCondition.MINT, tooShort, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("설명이 최대 길이보다 길면 예외가 발생한다")
    void testFrom_descriptionTooLong_throws() {
        // given
        String tooLong = "가".repeat(501);

        // when & then
        assertThatThrownBy(() -> ItemInfo.from(ItemCondition.MINT, tooLong, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미지가 최대 개수를 초과하면 예외가 발생한다")
    void testFrom_tooManyImages_throws() {
        // given
        List<String> images = List.of("1.png", "2.png", "3.png", "4.png", "5.png", "6.png");

        // when & then
        assertThatThrownBy(() -> ItemInfo.from(ItemCondition.MINT, "충분히 긴 설명입니다.", images)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 개수의 이미지는 유효하다")
    void testFrom_maxImageCount_succeeds() {
        // given
        List<String> images = List.of("1.png", "2.png", "3.png", "4.png", "5.png");

        // when
        ItemInfo itemInfo = ItemInfo.from(ItemCondition.MINT, "충분히 긴 설명입니다.", images);

        // then
        assertThat(itemInfo.getImageUrls()).hasSize(5);
    }

    @Test
    @DisplayName("전달받은 이미지 목록을 방어적으로 복사하여 외부 변경에 영향받지 않는다")
    void testFrom_defensivelyCopiesImageList() {
        // given
        List<String> mutableImages = new ArrayList<>(List.of("1.png"));
        ItemInfo itemInfo = ItemInfo.from(ItemCondition.MINT, "충분히 긴 설명입니다.", mutableImages);

        // when
        mutableImages.add("2.png");

        // then
        assertThat(itemInfo.getImageUrls()).containsExactly("1.png");
    }

    @Test
    @DisplayName("withDescription은 설명만 교체한 새 인스턴스를 반환한다")
    void testWithDescription_replacesOnlyDescription() {
        // given
        ItemInfo original = ItemInfo.from(ItemCondition.MINT, "원본 상품 설명입니다.", List.of("1.png"));

        // when
        ItemInfo updated = original.withDescription("변경된 상품 설명입니다.");

        // then
        assertThat(updated.getDescription()).isEqualTo("변경된 상품 설명입니다.");
        assertThat(updated.getCondition()).isEqualTo(original.getCondition());
        assertThat(updated.getImageUrls()).isEqualTo(original.getImageUrls());
        assertThat(original.getDescription()).isEqualTo("원본 상품 설명입니다.");
    }

    @Test
    @DisplayName("withCondition은 상태만 교체한 새 인스턴스를 반환한다")
    void testWithCondition_replacesOnlyCondition() {
        // given
        ItemInfo original = ItemInfo.from(ItemCondition.MINT, "원본 상품 설명입니다.", null);

        // when
        ItemInfo updated = original.withCondition(ItemCondition.GOOD);

        // then
        assertThat(updated.getCondition()).isEqualTo(ItemCondition.GOOD);
        assertThat(original.getCondition()).isEqualTo(ItemCondition.MINT);
    }
}
