package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;

import java.util.Optional;

public interface BankAccountRepository {

    Optional<BankAccount> findByMember(Member member);
}
