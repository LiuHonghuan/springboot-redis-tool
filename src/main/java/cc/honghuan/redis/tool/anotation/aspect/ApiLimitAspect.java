package cc.honghuan.redis.tool.anotation.aspect;

import cc.honghuan.redis.tool.anotation.ApiLimit;
import cc.honghuan.redis.tool.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author honghuan.Liu
 * @date 2021/9/17 11:33 上午
 */
@Slf4j
@Aspect
@Component
public class ApiLimitAspect {

    @Pointcut("@annotation(cc.honghuan.redis.tool.anotation.ApiLimit)")
    public void apiPointCut() {
    }


    @Around("apiPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ApiLimit limit = getAnnotation(joinPoint);
        if (limit == null) {
            return joinPoint.proceed();
        }
        // 这个key根据不同的业务来定义，比如用户Id，IP地址，接口方法
        String key = limit.key();
        // 限流接口
        Boolean result = RedisUtils.limitRate(key, limit.count(), limit.period());
        if (result) {
            return joinPoint.proceed();
        } else {
            throw new RuntimeException("接口访问次数受限");
        }
    }


    /**
     * 是否存在注解，如果存在就获取
     */
    private ApiLimit getAnnotation(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        if (method != null) {
            return method.getAnnotation(ApiLimit.class);
        }
        return null;
    }
}
