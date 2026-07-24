package site.coreservice.pointwallet.hold.infrastructure;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.pointwallet.hold.domain.Hold;
import site.coreservice.pointwallet.hold.domain.HoldRepository;

@Repository
@RequiredArgsConstructor
public class HoldRepositoryImpl implements HoldRepository {

    private final HoldJpaRepository holdJpaRepository;

    @Override
    public Hold save(Hold hold) {
        return holdJpaRepository.save(hold);
    }

    @Override
    public Optional<Hold> findByAuctionId(Long auctionId) {
        return holdJpaRepository.findByAuctionId(auctionId);
    }

    @Override
    public void delete(Hold hold) {
        holdJpaRepository.delete(hold);
    }
}