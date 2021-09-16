package cc.honghuan.redis.tool.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author honghuan.Liu
 * @date 2021/9/16 10:10 上午
 */
@Slf4j
@SuppressWarnings("all")
public class RedisUtils {

    private static RedisTemplate<String, Object> redisTemplate = SpringUtils.getBean("redisTemplate");


    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    public static void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public static Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public static Boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置key-value和过期时间 原子性操作
     *
     * @param key     键
     * @param value   值
     * @param timeout 超时时间
     * @return true成功 false失败
     */
    public static Boolean setnx(String key, Object value, Long timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 分布式锁，解锁 查询锁和解锁放在同一个原子性操作上。
     *
     * @param key
     * @param value
     * @return
     */
    public static Boolean unLock(String key, String value) {
        // 定义lua脚本
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";

        RedisScript redisScript = new DefaultRedisScript<>(script, Long.class);
        // 执行Lua脚本，传入脚本以及对应参数
        Object result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        if ("1".equals(result.toString())) {
            return true;
        }
        return false;
    }


    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return Long
     */
    public static Long incr(String key, Long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几
     * @return Long
     */
    public static Long decr(String key, Long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }


    /**
     * 扣减库存，查询和扣减操作在同一个原子性操作上。
     *
     * @param key    商品Id
     * @param number 扣减商品数量
     * @return
     */
    public static Long orderStock(String orderId, Integer number) {
        // 定义lua脚本
        String script =
                "local num = tonumber(ARGV[1]);" +
                        "local stock = tonumber(redis.call('get', KEYS[1]));" +
                        "if(stock >= num) then " +
                        "   return redis.call('incrby', KEYS[1], 0-num); " +
                        "end; " +
                        "return -1;";

        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        // 执行Lua脚本，传入脚本以及对应参数
        Long stockNumber = redisTemplate.execute(redisScript, Collections.singletonList(orderId), number);
        return stockNumber;
    }

}
