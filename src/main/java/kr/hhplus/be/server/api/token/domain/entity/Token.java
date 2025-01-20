package kr.hhplus.be.server.api.token.domain.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.api.common.type.TokenStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Desc :대기열 관리 및 콘서트 좌석 예매 권한을 부여하기 위한 Token
 */
@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 대기열 순서로 사용
    private String token; // token의 value
    private Long userId;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    private LocalDateTime expiredAt;
    private LocalDateTime maxExpiredAt; // 절대 만료 시점(연장 불가)
    private LocalDateTime createdAt;
    private LocalDateTime lastRequestAt; // 마지막 요청 시간 기록

    // 토큰 만료 여부 체크 (ACTIVE 상태에서만 적용)
    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isMaxExpired() {
        return maxExpiredAt != null && LocalDateTime.now().isAfter(maxExpiredAt);
    }

    // 활성화 상태로 변경
    public void activate(LocalDateTime expiration, LocalDateTime maxExpiration) {
        this.status = TokenStatus.ACTIVE;
        this.expiredAt = expiration;
        this.maxExpiredAt = maxExpiration;
    }

    // 상태 업데이트
    public void updateStatus(TokenStatus status) {
        this.status = status;
    }

    // 만료 시간 추가로 연장
    public void extendExpiration(int addMin) {

        LocalDateTime newExpiryTime = expiredAt.plusMinutes(addMin);

        // 절대 만료 시점(MaxExpiredAt)을 초과하지 않도록 제한
        if (newExpiryTime.isBefore(maxExpiredAt)) {
            this.expiredAt = newExpiryTime;
        } else {
            this.expiredAt = maxExpiredAt; // 초과 시 절대 만료 시점으로 설정
        }
    }

}
