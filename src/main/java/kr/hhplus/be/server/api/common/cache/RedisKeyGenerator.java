package kr.hhplus.be.server.api.common.cache;

import java.time.LocalDate;

/**
 * Redis 키 생성 전략을 관리하는 Util
 */
public class RedisKeyGenerator {

    private static final String DELIMITER = ":";

    // Concert Cache Keys
    public static String concertDatesKey(Long concertId) {
        return "concerts" + DELIMITER + concertId;
    }

    public static String concertSeatsKey(Long concertId, LocalDate scheduleDate) {
        return "concert" + DELIMITER + concertId + DELIMITER + "schedule" + DELIMITER + scheduleDate;
    }

    // Token Keys
    public static String tokenIdKey(Long tokenId) {
        return "token" + DELIMITER + "id" + DELIMITER + tokenId;
    }

    // Lock Keys
    public static String lockKey(String prefix, String key) {
        return "lock" + DELIMITER + prefix + key;
    }

    private RedisKeyGenerator() {
        throw new IllegalStateException("Utility class");
    }
}
