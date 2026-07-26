package site.coreservice.pointwallet.withdraw.infrastructure.client;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import site.coreservice.pointwallet.withdraw.application.port.BankAccount;
import site.coreservice.pointwallet.withdraw.application.port.MemberBankAccountPort;

@Component
@Profile("local")
public class MockMemberBankAccountClient implements MemberBankAccountPort {

    @Override
    public Optional<BankAccount> getBankAccount(Long memberId) {
        return Optional.of(new BankAccount("하나은행", "123-123456-12301", "홍길동"));
    }
}