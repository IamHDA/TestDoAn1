package com.vn.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.backend.services.impl.RedisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisServiceImpl Unit Tests")
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        // Mock redisTemplate.opsForX() calls
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("set - thành công gọi opsForValue().set")
    void set_Success() {
        redisService.set("key", "value");
        verify(valueOperations).set("key", "value");
    }

    @Test
    @DisplayName("get - thành công trả về giá trị từ opsForValue().get")
    void get_Success() {
        when(valueOperations.get("key")).thenReturn("value");
        String result = redisService.get("key", String.class);
        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("delete - thành công gọi redisTemplate.delete")
    void delete_Success() {
        when(redisTemplate.delete("key")).thenReturn(true);
        Boolean result = redisService.delete("key");
        assertThat(result).isTrue();
        verify(redisTemplate).delete("key");
    }

    @Test
    @DisplayName("hSet - thành công gọi opsForHash().put")
    void hSet_Success() {
        redisService.hSet("key", "field", "value");
        verify(hashOperations).put("key", "field", "value");
    }

    @Test
    @DisplayName("hGet - thành công trả về giá trị từ opsForHash().get")
    void hGet_Success() {
        when(hashOperations.get("key", "field")).thenReturn("value");
        String result = redisService.hGet("key", "field", String.class);
        assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("sAdd - thành công gọi opsForSet().add")
    void sAdd_Success() {
        redisService.sAdd("key", "value");
        verify(setOperations).add("key", "value");
    }

    @Test
    @DisplayName("expire - thành công gọi redisTemplate.expire")
    void expire_Success() {
        when(redisTemplate.expire("key", 60L, TimeUnit.SECONDS)).thenReturn(true);
        Boolean result = redisService.expire("key", 60L, TimeUnit.SECONDS);
        assertThat(result).isTrue();
        verify(redisTemplate).expire("key", 60L, TimeUnit.SECONDS);
    }
}
