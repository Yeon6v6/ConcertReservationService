package kr.hhplus.be.server.api.common.kafka.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {
    Optional<OutboxEntity> findByMessageKey(String messageKey);
    List<OutboxEntity> findByStatusInAndRetryCountLessThan(Collection<OutboxStatus> status, int retryCount);
}
