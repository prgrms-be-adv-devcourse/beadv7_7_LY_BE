package site.memberservice.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import site.memberservice.auth.domain.RefreshToken;
import site.memberservice.auth.domain.repository.RefreshTokenRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenIssuerTest {

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenIssuer refreshTokenIssuer;

    @BeforeEach
    void setUp() {
        this.refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        this.refreshTokenIssuer = new RefreshTokenIssuer(refreshTokenRepository);
    }

    @DisplayName("이미 리프레시 토큰이 있으면 값만 갱신해서 저장한다.")
    @Test
    void upsertUpdatesExistingRefreshToken() {
        // Given
        final Long memberId = 1727L;
        final RefreshToken existing = new RefreshToken(1L, "OLD_VALUE", memberId);

        given(refreshTokenRepository.findByMemberId(memberId))
            .willReturn(Optional.of(existing));
        given(refreshTokenRepository.save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        // When
        final RefreshToken result = refreshTokenIssuer.upsert(memberId, "NEW_VALUE");

        // Then
        assertThat(result.getValue()).isEqualTo("NEW_VALUE");
        verify(refreshTokenRepository).save(existing);
    }

    @DisplayName("리프레시 토큰이 없으면 새로 만들어 저장한다.")
    @Test
    void upsertCreatesNewRefreshTokenWhenAbsent() {
        // Given
        final Long memberId = 1727L;

        given(refreshTokenRepository.findByMemberId(memberId))
            .willReturn(Optional.empty());
        given(refreshTokenRepository.save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        // When
        final RefreshToken result = refreshTokenIssuer.upsert(memberId, "NEW_VALUE");

        // Then
        assertThat(result.getValue()).isEqualTo("NEW_VALUE");
        assertThat(result.getMemberId()).isEqualTo(memberId);
    }
}
