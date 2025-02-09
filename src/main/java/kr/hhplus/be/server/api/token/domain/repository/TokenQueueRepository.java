package kr.hhplus.be.server.api.token.domain.repository;

public interface TokenQueueRepository {
    void enqueue(Long tokenId, Long userId);
    void removeToken(Long tokenId);
    Long getQueuePosition(Long tokenId);
    Long getTokenByUserId(Long userId);
}
