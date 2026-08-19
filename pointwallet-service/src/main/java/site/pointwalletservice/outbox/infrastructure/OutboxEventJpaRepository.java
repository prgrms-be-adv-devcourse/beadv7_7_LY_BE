// outbox/infrastructure/OutboxEventJpaRepository.java
package site.pointwalletservice.outbox.infrastructure;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("select e from OutboxEvent e where e.status = :status order by e.createdAt asc")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);

    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxEventStatus status);
}