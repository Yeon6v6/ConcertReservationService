package kr.hhplus.be.server.api.token;

public class TokenConstants {
    public static final String TOKEN_ID_PREFIX = "token:id:";  // 토큰 ID 키에 사용
    
    public static final String TOKEN_QUEUE_KEY = "token:queue";  // 대기열 키
    public static final String ACTIVE_TOKENS_KEY = "token:active";  // 활성 토큰 저장소
    public static final String TOKEN_USER_KEY = "token:users";  // 사용자ID ↔ 토큰ID 매핑

    public static final long INITIAL_TTL_SECONDS = 600;  // 최초 TTL (10분)
    public static final long TTL_INCREMENT = 300;  // TTL 연장 단위 (5분)
    public static final long MAX_TTL_SECONDS = 1800;  // 최대 TTL (30분)
}
