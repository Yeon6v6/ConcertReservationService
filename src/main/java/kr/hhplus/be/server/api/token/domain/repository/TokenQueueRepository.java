package kr.hhplus.be.server.api.token.domain.repository;

import java.util.Set;

public interface TokenQueueRepository {
    void enqueue(Long tokenId, Long userId);
    void removeTokenQueue(Long tokenId);
    Long getQueuePosition(Long tokenId);
    Long getTokenByUserId(Long userId);
    boolean isUserInQueue(Long userId);
    Set<String> processQueue(int batchSize);
    void removeExpiredQueueEntries();
}
