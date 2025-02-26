package kr.hhplus.be.server.api.token.domain.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository{
    Long generateTokenId();
    void saveToken(Long tokenId, Long userId);
    void setTokenExpiration(Long tokenId, long expirationTime);
    boolean extendTokenTTL(Long tokenId);
    boolean isValidToken(Long tokenId);
    Long getQueuePosition(Long tokenId);
    void removeExpiredTokens();
    void deleteToken(Long tokenId);
    Long getTokenExpiration(Long tokenId);
}
