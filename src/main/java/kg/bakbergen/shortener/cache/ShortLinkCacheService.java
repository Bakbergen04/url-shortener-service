package kg.bakbergen.shortener.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkCacheService {

    private static final String KEY_PREFIX = "short-link:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<CachedShortLink> get(String code) {
        String key = cacheKey(code);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CachedShortLink.class));
        } catch (JsonProcessingException exception) {
            log.warn("Discarding malformed cached short link for code {}", code);
            evict(code);
            return Optional.empty();
        } catch (DataAccessException exception) {
            log.warn("Redis read failed for short link {}: {}", code, exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(String code, CachedShortLink cachedValue, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(cachedValue);
            redisTemplate.opsForValue().set(cacheKey(code), json, ttl);
        } catch (JsonProcessingException exception) {
            log.warn("Could not serialize short link {} for caching", code);
        } catch (DataAccessException exception) {
            log.warn("Redis write failed for short link {}: {}", code, exception.getMessage());
        }
    }

    public void evict(String code) {
        try {
            redisTemplate.delete(cacheKey(code));
        } catch (DataAccessException exception) {
            log.warn("Redis eviction failed for short link {}: {}", code, exception.getMessage());
        }
    }

    private String cacheKey(String code) {
        return KEY_PREFIX + code;
    }
}
