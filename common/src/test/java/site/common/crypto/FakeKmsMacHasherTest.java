package site.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.common.exception.BusinessException;

@DisplayName("FakeKmsMacHasher")
class FakeKmsMacHasherTest {

    private final FakeKmsMacHasher hasher = new FakeKmsMacHasher();

    @Test
    @DisplayName("같은 입력이면 항상 같은 해시를 돌려준다 — 블라인드 인덱스가 로컬에서도 의미가 있으려면 결정적이어야 한다")
    void 같은_입력이면_같은_해시를_돌려준다() {
        final String first = hasher.hash("010-1234-5678");
        final String second = hasher.hash("010-1234-5678");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("다른 입력이면 다른 해시를 돌려준다")
    void 다른_입력이면_다른_해시를_돌려준다() {
        final String first = hasher.hash("010-1234-5678");
        final String second = hasher.hash("010-9999-5678");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("plaintext가 null이면 BusinessException을 던진다")
    void plaintext가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> hasher.hash(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("plaintext가 공백이면 BusinessException을 던진다")
    void plaintext가_공백이면_예외를_던진다() {
        assertThatThrownBy(() -> hasher.hash("   "))
            .isInstanceOf(BusinessException.class);
    }
}
