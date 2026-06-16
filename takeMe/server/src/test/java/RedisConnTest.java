//import com.me.redis.utils.RedisUtil;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import java.util.concurrent.TimeUnit;
//
//// 填入你server模块的SpringBoot启动类.class
//@SpringBootTest(classes = com.me.TakeMeApplication.class)
//public class RedisConnTest {
//
//    @Autowired
//    private RedisUtil redisUtil;
//
//    @Test
//    void testLink() {
//        String testKey = "test:project:redis";
//        String testVal = "连接成功";
//        redisUtil.set(testKey, testVal, 10, TimeUnit.MINUTES);
//
//        Object val = redisUtil.get(testKey);
//        System.out.println("读取结果：" + val);
//        System.out.println("key是否存在：" + redisUtil.hasKey(testKey));
//    }
//}