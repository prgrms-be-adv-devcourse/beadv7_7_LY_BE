package site.pointwalletservice.hold.infrastructure;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.pointwalletservice.hold.domain.Hold;
import site.pointwalletservice.hold.domain.HoldRepository;

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

    @Override
    public Optional<Hold> findByAuctionIdForUpdate(Long auctionId) {
        return holdJpaRepository.findByAuctionIdForUpdate(auctionId);
    }
}