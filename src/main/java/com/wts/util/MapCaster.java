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
    @SuppressWarnings("unchecked")
    public Map<String, Object> safeMapCast(Object obj) {
        if (obj == null) {
            log.debug("safeMapCast: input object is null");
            return null;
        }

        log.debug("safeMapCast: input object type = {}, value = {}", obj.getClass().getName(), obj);

        // LinkedHashMap과 HashMap 등 Map 구현체들을 모두 처리
        if (obj instanceof Map) {
            try {
                Map<?, ?> rawMap = (Map<?, ?>) obj;
                // key가 String이 아닌 경우에 대한 방어 코드
                Map<String, Object> result = new java.util.HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    String key = entry.getKey() != null ? entry.getKey().toString() : null;
                    if (key != null) {
                        result.put(key, entry.getValue());
                    }
                }
                log.debug("safeMapCast: successfully converted to Map<String, Object>, size = {}", result.size());
                return result;
            } catch (ClassCastException e) {
                log.warn("safeMapCast: ClassCastException when casting to Map", e);
                return null;
            }
        }

        log.warn("safeMapCast: object is not a Map instance, type = {}", obj.getClass().getName());
        return null;
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
