package cc.honghuan.redis.tool.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author honghuan.Liu
 * @date 2021/9/17 11:29 上午
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLimit {

    String name() default "";

    // 资源 key
    String key() default "";

    // 时间，单位秒
    int period();

    // 限制访问次数
    int count();

}
