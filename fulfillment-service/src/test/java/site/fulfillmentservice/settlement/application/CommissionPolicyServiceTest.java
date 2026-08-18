package site.fulfillmentservice.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import site.fulfillmentservice.settlement.application.dto.CommissionPolicyResult;
import site.fulfillmentservice.settlement.application.dto.CreateCommissionPolicyCommand;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;
import site.fulfillmentservice.settlement.exception.SettlementErrorCode;
import site.fulfillmentservice.settlement.exception.SettlementException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionPolicyService")
class CommissionPolicyServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private CommissionPolicyRepository commissionPolicyRepository;

    @InjectMocks
    private CommissionPolicyService commissionPolicyService;

    @Captor
    private ArgumentCaptor<CommissionPolicy> commissionPolicyCaptor;

    @Nested
    @DisplayName("createCommissionPolicy")
    class CreateCommissionPolicy {

        @Test
        @DisplayName("열린 정책이 없으면 predecessor 처리 없이 신규 정책을 저장한다")
        void createsFirstPolicyWhenNoOpenPolicyExists() {
            // given
            LocalDate effectiveFromDate = LocalDate.now().plusDays(1);
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), effectiveFromDate);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.empty());
            given(commissionPolicyRepository.save(commissionPolicyCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CommissionPolicyResult result = commissionPolicyService.createCommissionPolicy(command, ADMIN_ID);

            // then
            CommissionPolicy saved = commissionPolicyCaptor.getValue();
            assertThat(saved.getCommissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            assertThat(saved.getEffectiveFrom()).isEqualTo(effectiveFromDate.atStartOfDay());
            assertThat(saved.getEffectiveTo()).isNull();
            assertThat(result.commissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            verify(commissionPolicyRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("현재 적용 중인 정책이 있으면 predecessor를 닫고 신규 정책을 저장한다")
        void closesActivePredecessorAndCreatesNewPolicy() {
            // given
            LocalDate effectiveFromDate = LocalDate.now().plusDays(1);
            LocalDateTime effectiveFrom = effectiveFromDate.atStartOfDay();
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), effectiveFromDate);
            CommissionPolicy activePolicy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(10), null);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.of(activePolicy));
            given(commissionPolicyRepository.saveAndFlush(activePolicy)).willReturn(activePolicy);
            given(commissionPolicyRepository.save(commissionPolicyCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            commissionPolicyService.createCommissionPolicy(command, ADMIN_ID);

            // then
            assertThat(activePolicy.getEffectiveTo()).isEqualTo(effectiveFrom);
            verify(commissionPolicyRepository).saveAndFlush(activePolicy);
            assertThat(commissionPolicyCaptor.getValue().getEffectiveFrom()).isEqualTo(effectiveFrom);
        }

        @Test
        @DisplayName("pending 정책이 이미 있으면 등록을 거부한다")
        void rejectsWhenPendingPolicyAlreadyExists() {
            // given
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), LocalDate.now().plusDays(2));
            CommissionPolicy pendingPolicy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().plusDays(1), null);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.of(pendingPolicy));

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.createCommissionPolicy(command, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.PENDING_COMMISSION_POLICY_ALREADY_EXISTS);
            verify(commissionPolicyRepository, never()).save(any());
            verify(commissionPolicyRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("effectiveFromDate가 오늘이거나 과거면 등록을 거부한다")
        void rejectsWhenEffectiveFromDateIsNotAfterToday() {
            // given
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), LocalDate.now());

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.createCommissionPolicy(command, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.INVALID_COMMISSION_POLICY_EFFECTIVE_FROM_DATE);
            verify(commissionPolicyRepository, never()).findByEffectiveToIsNull();
        }

        @Test
        @DisplayName("effectiveFromDate가 null이면 NPE 대신 등록을 거부한다")
        void rejectsWhenEffectiveFromDateIsNull() {
            // given
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), null);

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.createCommissionPolicy(command, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.INVALID_COMMISSION_POLICY_EFFECTIVE_FROM_DATE);
            verify(commissionPolicyRepository, never()).findByEffectiveToIsNull();
        }

        @Test
        @DisplayName("predecessor 저장 중 낙관적 락 충돌이 나면 정책 충돌 예외로 변환한다")
        void translatesOptimisticLockConflictToConflictException() {
            // given
            CreateCommissionPolicyCommand command =
                    new CreateCommissionPolicyCommand(BigDecimal.valueOf(0.1000), LocalDate.now().plusDays(1));
            CommissionPolicy activePolicy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(10), null);
            given(commissionPolicyRepository.findByEffectiveToIsNull()).willReturn(Optional.of(activePolicy));
            given(commissionPolicyRepository.saveAndFlush(activePolicy))
                    .willThrow(new ObjectOptimisticLockingFailureException(CommissionPolicy.class, 1L));

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.createCommissionPolicy(command, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
            verify(commissionPolicyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCommissionPolicy")
    class DeleteCommissionPolicy {

        @Test
        @DisplayName("predecessor가 있으면 reopen하고 대상을 삭제한다")
        void deletesTargetAndReopensPredecessor() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().plusDays(1);
            CommissionPolicy target = CommissionPolicy.of(BigDecimal.valueOf(0.1000), effectiveFrom, null);
            CommissionPolicy predecessor = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(10), effectiveFrom);
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.of(target));
            given(commissionPolicyRepository.findByEffectiveTo(effectiveFrom)).willReturn(Optional.of(predecessor));
            given(commissionPolicyRepository.saveAndFlush(predecessor)).willReturn(predecessor);

            // when
            commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID);

            // then
            assertThat(predecessor.getEffectiveTo()).isNull();
            verify(commissionPolicyRepository).deleteAndFlush(target);
        }

        @Test
        @DisplayName("predecessor가 없으면 대상만 삭제한다")
        void deletesTargetWithoutPredecessor() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().plusDays(1);
            CommissionPolicy target = CommissionPolicy.of(BigDecimal.valueOf(0.1000), effectiveFrom, null);
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.of(target));
            given(commissionPolicyRepository.findByEffectiveTo(effectiveFrom)).willReturn(Optional.empty());

            // when
            commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID);

            // then
            verify(commissionPolicyRepository, never()).saveAndFlush(any());
            verify(commissionPolicyRepository).deleteAndFlush(target);
        }

        @Test
        @DisplayName("존재하지 않는 id면 예외를 던진다")
        void rejectsWhenNotFound() {
            // given
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.COMMISSION_POLICY_NOT_FOUND);
            verify(commissionPolicyRepository, never()).deleteAndFlush(any());
        }

        @Test
        @DisplayName("적용 중이거나 과거인 정책은 삭제를 거부한다")
        void rejectsWhenNotPending() {
            // given
            CommissionPolicy activePolicy = CommissionPolicy.of(
                    BigDecimal.valueOf(0.1000), LocalDateTime.now().minusDays(1), null);
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.of(activePolicy));

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.COMMISSION_POLICY_NOT_DELETABLE);
            verify(commissionPolicyRepository, never()).deleteAndFlush(any());
        }

        @Test
        @DisplayName("predecessor 저장 중 낙관적 락 충돌이 나면 정책 충돌 예외로 변환한다")
        void translatesOptimisticLockConflictToConflictException() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().plusDays(1);
            CommissionPolicy target = CommissionPolicy.of(BigDecimal.valueOf(0.1000), effectiveFrom, null);
            CommissionPolicy predecessor = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(10), effectiveFrom);
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.of(target));
            given(commissionPolicyRepository.findByEffectiveTo(effectiveFrom)).willReturn(Optional.of(predecessor));
            given(commissionPolicyRepository.saveAndFlush(predecessor))
                    .willThrow(new ObjectOptimisticLockingFailureException(CommissionPolicy.class, 2L));

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
            verify(commissionPolicyRepository, never()).deleteAndFlush(any());
        }

        @Test
        @DisplayName("target 삭제 중 낙관적 락 충돌이 나면 정책 충돌 예외로 변환한다")
        void translatesTargetDeleteOptimisticLockConflictToConflictException() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().plusDays(1);
            CommissionPolicy target = CommissionPolicy.of(BigDecimal.valueOf(0.1000), effectiveFrom, null);
            given(commissionPolicyRepository.findById(1L)).willReturn(Optional.of(target));
            given(commissionPolicyRepository.findByEffectiveTo(effectiveFrom)).willReturn(Optional.empty());
            willThrow(new ObjectOptimisticLockingFailureException(CommissionPolicy.class, 1L))
                    .given(commissionPolicyRepository).deleteAndFlush(target);

            // when & then
            assertThatThrownBy(() -> commissionPolicyService.deleteCommissionPolicy(1L, ADMIN_ID))
                    .isInstanceOf(SettlementException.class)
                    .extracting(e -> ((SettlementException) e).getErrorCode())
                    .isEqualTo(SettlementErrorCode.COMMISSION_POLICY_CONFLICT);
        }
    }

    @Nested
    @DisplayName("getCommissionPolicies")
    class GetCommissionPolicies {

        @Test
        @DisplayName("전체 정책을 effectiveFrom 내림차순으로 반환한다")
        void returnsAllPoliciesOrderedByEffectiveFromDesc() {
            // given
            CommissionPolicy newer = CommissionPolicy.of(
                    BigDecimal.valueOf(0.1000), LocalDateTime.now().minusDays(1), null);
            CommissionPolicy older = CommissionPolicy.of(
                    BigDecimal.valueOf(0.0500), LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));
            given(commissionPolicyRepository.findAllByOrderByEffectiveFromDesc())
                    .willReturn(List.of(newer, older));

            // when
            List<CommissionPolicyResult> results = commissionPolicyService.getCommissionPolicies();

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).commissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.1000));
            assertThat(results.get(1).commissionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0500));
        }

        @Test
        @DisplayName("정책이 하나도 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoPolicies() {
            // given
            given(commissionPolicyRepository.findAllByOrderByEffectiveFromDesc()).willReturn(List.of());

            // when
            List<CommissionPolicyResult> results = commissionPolicyService.getCommissionPolicies();

            // then
            assertThat(results).isEmpty();
        }
    }
}
