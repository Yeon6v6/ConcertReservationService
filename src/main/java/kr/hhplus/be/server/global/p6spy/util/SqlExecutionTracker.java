package kr.hhplus.be.server.global.p6spy.util;

import java.util.HashMap;
import java.util.Map;

public class SqlExecutionTracker {
    private static final ThreadLocal<Map<Integer, Boolean>> sqlExecutedForConnection = new ThreadLocal<>();

    public static void markExecuted(int connectionId) {
        Map<Integer, Boolean> executedMap = sqlExecutedForConnection.get();
        if (executedMap == null) {
            executedMap = new HashMap<>();
            sqlExecutedForConnection.set(executedMap);
        }
        executedMap.put(connectionId, true);
    }

    public static boolean hasExecuted(int connectionId) {
        Map<Integer, Boolean> executedMap = sqlExecutedForConnection.get();
        if (executedMap == null) {
            return false;
        }
        return executedMap.getOrDefault(connectionId, false);
    }

    public static void clearForConnection(int connectionId) {
        Map<Integer, Boolean> executedMap = sqlExecutedForConnection.get();
        if (executedMap != null) {
            executedMap.remove(connectionId);
            // 모든 연결 정보가 제거되었으면 ThreadLocal도 클리어
            if (executedMap.isEmpty()) {
                sqlExecutedForConnection.remove();
            }
        }
    }
    public static void clear() {
        sqlExecutedForConnection.remove();
    }
}

