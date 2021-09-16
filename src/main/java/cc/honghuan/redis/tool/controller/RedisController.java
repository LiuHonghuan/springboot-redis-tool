package cc.honghuan.redis.tool.controller;

import cc.honghuan.redis.tool.utils.RedisUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author honghuan.Liu
 * @date 2021/9/16 10:13 上午
 */
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

    @GetMapping("/get")
    public String set(String key) {
        Object o = RedisUtils.get(key);
        return Optional.ofNullable(o).orElse("ok").toString();
    }

    @GetMapping("/del")
    public String del(String key) {
        RedisUtils.del(key);
        return "ok";
    }

}

