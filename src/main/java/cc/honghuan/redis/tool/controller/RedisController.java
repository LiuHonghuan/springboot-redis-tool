package cc.honghuan.redis.tool.controller;

import cc.honghuan.redis.tool.utils.RedisUtils;
import cc.honghuan.redis.tool.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author honghuan.Liu
 * @date 2021/9/16 10:13 上午
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
public class RedisController {


    @GetMapping("/")
    public String test() {
        return "test";
    }

    @GetMapping("/set")
    public String set(String key, String value) {
        RedisUtils.set(key, value);
        return "ok";
    }

    private int stockNum = 1000;


    @GetMapping("/distributed/lock")
    public String lock() {
        // 业务key，变化的
        String key = "lock-key";
        String uuid = UUID.randomUUID().toString();
        try {
            Boolean flag = RedisUtils.setnx(key, uuid, 5L);
            // 如果拿到锁
            if (flag) {
                log.info("start business work");
                // 模拟业务操作 ，扣减库存
                if (stockNum > 0) {
                    ThreadUtils.sleep(1000L);
                    stockNum--;
                }
            }
        } finally {
            // 释放锁
            RedisUtils.unLock(key, uuid);
        }
        return "ok";
    }


}

