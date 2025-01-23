package kr.hhplus.be.server.api.common.lock.annotation;

import jdk.jshell.MethodSnippet;
import kr.hhplus.be.server.api.common.exception.CustomException;
import kr.hhplus.be.server.api.common.lock.RedisLockManager;
import kr.hhplus.be.server.api.concert.exception.SeatErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@RequiredArgsConstructor
@Component
@Aspect
@Order(1)
public class RedisLockAspect {
    private static final String REDIS_LOCK_PREFIX = "lock:";

    private final RedisLockManager redisLockManager;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(redisLock)")
    public Object getLock(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisLock annotation = method.getAnnotation(RedisLock.class);

        String lockKey = REDIS_LOCK_PREFIX
                + annotation.prefix()
                + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), annotation.key());

        // Redis 락
        boolean rLock = redisLockManager.lock(lockKey, 10); // 10초 동안 락 유지

        try{
            if (!rLock) {
                throw new CustomException(SeatErrorCode.SEAT_LOCKED); // 락 획득 실패
            }
            return aopForTransaction.proceed(joinPoint);

        }finally{
            // 락 해제
            redisLockManager.unlock(lockKey);
        }
    }
}
