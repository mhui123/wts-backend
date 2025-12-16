package com.wts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class MapCaster {
    /**
     * 제네릭을 활용한 타입 안전 캐스팅 - 다양한 타입에 대응
     */
    private <T> T safeCast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return null;
    }

    /**
     * Map 타입 안전 추출 - @SuppressWarnings 제거
     */
    public Map<String, Object> safeMapCast(Object obj) {
        return safeCast(obj, Map.class);
    }

    /**
     * String 타입 안전 추출
     */
    private String safeStringCast(Object obj) {
        return safeCast(obj, String.class);
    }

    /**
     * Integer 타입 안전 추출 및 변환
     */
    private Integer safeIntegerExtract(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Integer) {
            return (Integer) obj;
        }

        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse integer from string: {}", obj);
                return null;
            }
        }

        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }

        return null;
    }

    /**
     * Map에서 타입 안전하게 값 추출
     */
    private <T> T safeMapGet(Map<String, Object> map, String key, Class<T> clazz) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return safeCast(value, clazz);
    }

    /**
     * Map에서 String 값 안전 추출
     */
    public String safeMapGetString(Map<String, Object> map, String key) {
        return safeMapGet(map, key, String.class);
    }

    /**
     * Map에서 Integer 값 안전 추출 (문자열 파싱 포함)
     */
    public Integer safeMapGetInteger(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return safeIntegerExtract(value);
    }
}
