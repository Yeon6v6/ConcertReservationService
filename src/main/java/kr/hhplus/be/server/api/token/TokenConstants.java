package kr.hhplus.be.server.api.token;

public class TokenConstants {
    public static final String TOKEN_ID_PREFIX = "token:id:";  // 토큰 ID 키에 사용

    public static final String TOKEN_NAMESPACE = "token:data";  // 통합된 네임스페이스
    public static final String TOKEN_QUEUE_PREFIX = "queue:";  // 대기열 항목
    public static final String TOKEN_ACTIVE_PREFIX = "active:";  // 활성 토큰
    public static final String TOKEN_USER_PREFIX = "user:";  // 사용자 매핑 (userId -> tokenId)
    public static final String TOKEN_TO_USER_PREFIX = "token:user:";  // 역색인 (tokenId -> userId)

    public static final long INITIAL_TTL_SECONDS = 600;  // 최초 TTL (10분)
    public static final long TTL_INCREMENT = 300;  // TTL 연장 단위 (5분)
    public static final long MAX_TTL_SECONDS = 1800;  // 최대 TTL (30분)
    public static final long QUEUE_TTL_SECONDS = 3600;  // 대기열 TTL (1시간)
}
