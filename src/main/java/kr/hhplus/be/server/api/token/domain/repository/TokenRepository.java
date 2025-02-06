package kr.hhplus.be.server.api.token.domain.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository{
    Long generateTokenId();
    void saveToken(Long tokenId, Long userId, String tokenValue);
    void saveTokenMapping(String tokenValue, Long tokenId);
    void deleteToken(Long tokenId);
}
