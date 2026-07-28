package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;

import java.util.List;

public interface BankAccountJpaRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByMember(Member member);
}
