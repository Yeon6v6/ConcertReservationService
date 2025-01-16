package kr.hhplus.be.server.api.user.application.dto.response;

import kr.hhplus.be.server.api.user.domain.entity.User;

public record UserBalanceResult(
        Long userId,
        Long balance
) {
    public static UserBalanceResult from(User user){
        return new UserBalanceResult(user.getId(), user.getBalance());
    }
}
