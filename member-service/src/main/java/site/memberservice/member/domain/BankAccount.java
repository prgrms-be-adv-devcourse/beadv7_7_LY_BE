package site.memberservice.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.common.entity.BaseEntity;
import site.memberservice.member.exception.MemberException;

import java.util.regex.Pattern;

import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "bank_account",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_id_account_number",
            columnNames = {"member_id", "account_number"}
        ),
    }
)
@Entity
public class BankAccount extends BaseEntity {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]+(-[0-9]+)*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", length = 255, nullable = false)
    private String accountNumber;

    @Column(name = "bank_name", length = 50, nullable = false)
    private String bankName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public BankAccount(
        final Long id,
        final String accountNumber,
        final String bankName,
        final Member member
    ) {
        validateAccountNumber(accountNumber);
        validateBankName(bankName);
        validateMember(member);

        this.id = id;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.member = member;
    }

    public static BankAccount create(
        final String accountNumber,
        final String bankName,
        final Member member
    ) {
        return new BankAccount(null, accountNumber, bankName, member);
    }

    private void validateAccountNumber(final String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, "은행 계좌 번호는 null 혹은 공백일 수 없습니다.");
        }

        if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new MemberException(INVALID_MEMBER_INFO, "유효하지 않은 형태의 은행 계좌 번호입니다.");
        }
    }

    private void validateBankName(final String bankName) {
        if (bankName == null || bankName.isBlank()) {
            throw new MemberException(INVALID_MEMBER_INFO, "은행명은 null 혹은 공백일 수 없습니다.");
        }
    }

    private void validateMember(final Member member) {
        if (member == null) {
            throw new MemberException(INVALID_MEMBER_INFO, "회원 정보는 null일 수 없습니다.");
        }
    }
}
