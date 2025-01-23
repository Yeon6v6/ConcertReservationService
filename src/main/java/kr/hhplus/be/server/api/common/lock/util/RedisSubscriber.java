package kr.hhplus.be.server.api.common.lock.util;

import org.springframework.stereotype.Component;

public interface RedisSubscriber {
    void handleMessage(String message);
}
