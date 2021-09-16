## springboot-redis-tool
`springboot`整合`redis` 实现**分布式锁**，**redis限流**，解决**高并发超卖**问题。



***
### 安装启动

修改`application.yml`配置文件后，项目可正常运行。

![image-20210916174315383](https://gitee.com/honghuan921/pic/raw/master/image-20210916174315383.png)

***
### 实现

* 分布式锁

  ```java
  @GetMapping("/distributed/lock")
    public String lock(Long orderId) {
        if(Objects.nonNull(orderId)){
            throw new RuntimeException("orderId not be null");
        }
        String uuid = UUID.randomUUID().toString();
        try {
            Boolean flag = RedisUtils.setnx(orderId.toString(), uuid, 5L);
            // 如果拿到锁
            if (flag) {
                log.info("start business work");
                // 模拟业务操作，扣减库存
                if (stockNum > 0) {
                    ThreadUtils.sleep(1000L);
                    stockNum--;
                }
            }
        } finally {
            // 释放锁
            RedisUtils.unLock(orderId.toString(), uuid);
        }
        return "ok";
    }
  ```



* 高并发超卖问题
  * 方式1：使用**分布式锁**对商品库存进行扣减操作。
  * 方式2：商品库存数量存放在redis中，使用lua表达式**对库存查询和扣减做原子性操作**。（推荐）

  ```java
      @GetMapping("/seckillStock")
      public String seckillStock() {
          int i = ThreadLocalRandom.current().nextInt(1, 4);
          // 预判断作用；库存不足时，仍然还会进入判断条件中，但实际redis中的库存不会超卖
          Integer stockNumber = (Integer) RedisUtils.get("stock");
          if (stockNumber > 0) {
              // 扣减后的库存，stock大于等于0的话，说明秒杀成功；stock为 -1，表示秒杀失败。
              Long stock = RedisUtils.orderStock("stock", i);
              log.info("扣减库存{}, 当前剩余库存 {}", i, stock < 0 ? "不足" : stock);
          }
          return "ok";
      }
  ```



* redis限流



### 核心工具类

```java
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

```








