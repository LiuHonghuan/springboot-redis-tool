package cc.honghuan.redis.tool.controller;

import cc.honghuan.redis.tool.utils.RedisUtils;
import cc.honghuan.redis.tool.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author honghuan.Liu
 * @date 2021/9/16 10:13 上午
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private int stockNum = 1000;

    /**
     * 初始化库存
     */
    @PostConstruct
    public void initStock() {
        Boolean stock = RedisUtils.set("stock", 1000);
        log.info("init stock result {}", stock);
    }

    /**
     * 秒杀库存超卖
     * @return
     */
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

    /**
     * 分布式锁
     * @return
     */
    @GetMapping("/distributed/lock")
    public String lock(Long orderId) {
        if (Objects.nonNull(orderId)) {
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


}

