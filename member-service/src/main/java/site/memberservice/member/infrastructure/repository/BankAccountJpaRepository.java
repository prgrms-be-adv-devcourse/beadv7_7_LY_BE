package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;

import java.util.Optional;

public interface BankAccountJpaRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByMember(Member member);
}
