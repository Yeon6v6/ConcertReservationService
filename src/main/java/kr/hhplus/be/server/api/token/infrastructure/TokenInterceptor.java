package kr.hhplus.be.server.api.token.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.hhplus.be.server.api.token.domain.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {
    private final TokenRepository tokenRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tokenIdStr = request.getHeader("Authorization");
        if (tokenIdStr == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        try {
            Long tokenId = Long.parseLong(tokenIdStr);

            // TTL 검증 (연장 없이 단순 검증)
            if (!tokenRepository.isValidToken(tokenId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }

            // TTL 5분 연장 (모든 요청에서 연장)
            boolean extended = tokenRepository.extendTokenTTL(tokenId);
            if (!extended) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
    }
}
