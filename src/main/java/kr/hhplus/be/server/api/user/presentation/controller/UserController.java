package kr.hhplus.be.server.api.user.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.api.user.application.dto.response.UserBalanceResult;
import kr.hhplus.be.server.api.user.application.service.UserService;
import kr.hhplus.be.server.api.user.presentation.dto.BalanceResponse;
import kr.hhplus.be.server.api.user.presentation.dto.ChargeRequest;
import kr.hhplus.be.server.api.common.response.ApiResponse;
import kr.hhplus.be.server.api.common.response.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/balance")
@RequiredArgsConstructor
@Tag(name = "User Balance", description =  "사용자 잔액 API - 잔액 조회 및 충전")
public class UserController {

    private final UserService userService;

    /**
     * 잔액 조회 API
     * @param userId
     * @return 사용자 잔액 정보
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseDTO<BalanceResponse>> getBalance(@PathVariable Long userId) {
        UserBalanceResult userBalnaceResult = userService.getBalance(userId);

        BalanceResponse response = new BalanceResponse(userBalnaceResult.userId(), userBalnaceResult.balance());

        return ApiResponse.success("잔액 조회 성공", response);
    }

    /**
     * 잔액 충전 API
     * @param request
     * @return 성공 메시지
     */
    @PostMapping("/charge")
    public ResponseEntity<ResponseDTO<BalanceResponse>> rechargeBalance(@RequestBody ChargeRequest request) {
        // 충전 후의 현재 잔액 계산
        Long updatedAmount = userService.chargeBalance(request.getUserId(), request.getAmount());

        BalanceResponse response = new BalanceResponse(request.getUserId(), updatedAmount);

        return ApiResponse.success("잔액이 충전되었습니다.", response);
    }
}