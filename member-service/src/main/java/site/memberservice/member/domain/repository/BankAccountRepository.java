package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;

import java.util.List;

public interface BankAccountRepository {

    List<BankAccount> findAllByMember(Member member);
}
