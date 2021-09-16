package cc.honghuan.redis.tool.utils;

/**
 * @author honghuan.Liu
 * @date 2021/9/16 3:26 下午
 */
public class ThreadUtils {

    public static void sleep(Long sec) {
        try {
            Thread.sleep(sec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
