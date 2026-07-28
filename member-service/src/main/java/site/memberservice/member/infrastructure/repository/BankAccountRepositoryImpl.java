package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.repository.BankAccountRepository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final BankAccountJpaRepository bankAccountJpaRepository;

    @Override
    public List<BankAccount> findAllByMember(final Member member) {
        return bankAccountJpaRepository.findAllByMember(member);
    }
}
