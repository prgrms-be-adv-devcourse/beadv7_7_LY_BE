package site.auctionservice.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * AuctionRepositoryImpl.findByIdForUpdate()의 락 대기시간 설정 + 예외 번역 로직을 검증한다.
 * AuctionPessimisticLockTest(실제 H2 DB)로는 이걸 검증할 수 없다 - setLockWaitTimeout()이 실행하는
 * "SET innodb_lock_wait_timeout"은 MySQL 전용 세션 변수라 H2에서 SQLGrammarException이 난다
 * (WalletLockConcurrencyTest가 WalletApplicationService를 거치지 않고 WalletJpaRepository를
 * 직접 호출하는 것과 동일한 제약 - AuctionPessimisticLockTest 클래스 주석 참고). 그래서 여기서는
 * AuctionJpaRepository를 mock으로 대체해 "호출 순서"와 "예외 번역"만 검증하고, 실제 MySQL에서
 * 3초 뒤 정말로 실패하는지는 이 테스트의 범위 밖이다.
 */
@ExtendWith(MockitoExtension.class)
class AuctionRepositoryImplLockTest {

    @Mock
    private AuctionJpaRepository jpaRepository;

    @InjectMocks
    private AuctionRepositoryImpl auctionRepositoryImpl;

    @Test
    @DisplayName("findByIdForUpdate()는 락을 잡기 전에 락 대기 상한선(3초)을 먼저 설정한다")
    void findByIdForUpdate_setsLockWaitTimeoutBeforeLocking() {
        // given
        given(jpaRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // when
        auctionRepositoryImpl.findByIdForUpdate(1L);

        // then
        var inOrder = org.mockito.Mockito.inOrder(jpaRepository);
        inOrder.verify(jpaRepository).setLockWaitTimeout(3);
        inOrder.verify(jpaRepository).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("락 대기시간을 넘겨 PessimisticLockingFailureException이 나면 AuctionException(LOCK_ACQUISITION_FAILED)으로 번역한다")
    void findByIdForUpdate_lockContention_translatesToAuctionException() {
        // given
        willThrow(new PessimisticLockingFailureException("lock not available"))
                .given(jpaRepository).findByIdForUpdate(1L);

        // when & then
        assertThatThrownBy(() -> auctionRepositoryImpl.findByIdForUpdate(1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.LOCK_ACQUISITION_FAILED);
        verify(jpaRepository).setLockWaitTimeout(3);
    }

    @Test
    @DisplayName("락을 정상적으로 잡으면 조회 결과를 그대로 반환한다")
    void findByIdForUpdate_success_returnsResult() {
        // given
        given(jpaRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        // when
        var result = auctionRepositoryImpl.findByIdForUpdate(1L);

        // then
        assertThat(result).isEmpty();
    }
}
