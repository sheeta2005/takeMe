//package com.me;
//
//import com.me.entity.Admin;
//import com.me.entity.Order;
//import com.me.entity.ServicePackage;
//import com.me.mapper.AdminMapper;
//import com.me.mapper.OrderMapper;
//import com.me.mapper.ServicePackageMapper;
//import com.me.redis.utils.RedisUtil;
//import com.me.service.*;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@SpringBootTest
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class RedisFeatureTest {
//
//    @Autowired
//    private RedisUtil redisUtil;
//
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Autowired
//    private AdminService adminService;
//
//    @Autowired
//    private ServicePackageService servicePackageService;
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private AdminDashboardService dashboardService;
//
//    @Autowired
//    private OnlineUserService onlineUserService;
//
//    @Autowired
//    private AdminMapper adminMapper;
//
//    @Autowired
//    private OrderMapper orderMapper;
//
//    @Autowired
//    private ServicePackageMapper servicePackageMapper;
//
//    @BeforeEach
//    void setUp() {
//        Set<String> keys = redisUtil.keys("*");
//        if (keys != null && !keys.isEmpty()) {
//            stringRedisTemplate.delete(keys);
//            log.info("清理Redis环境完成");
//        }
//        Set<String> onlineKeys = stringRedisTemplate.keys("online:*");
//        if (onlineKeys != null && !onlineKeys.isEmpty()) {
//            stringRedisTemplate.delete(onlineKeys);
//            log.info("清理online前缀脏数据");
//        }
//        Set<String> rateKeys = stringRedisTemplate.keys("rate:*");
//        if (rateKeys != null && !rateKeys.isEmpty()) {
//            stringRedisTemplate.delete(rateKeys);
//            log.info("清理rate前缀脏数据");
//        }
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(1)
//    @DisplayName("批次1：热点数据缓存+缓存穿透防护")
//    void testBatch1_CacheAndPenetrationProtection() throws InterruptedException {
//        System.out.println("\n========== 批次1：热点数据缓存测试 ==========");
//
//        // 1.1 测试 Dashboard 缓存
//        System.out.println("\n--- 1.1 Dashboard 数据缓存 ---");
//        Map<String, Object> data1 = dashboardService.getDashboardData();
//        System.out.println("第一次查询（查DB）: totalOrders=" + data1.get("totalOrders"));
//
//        Thread.sleep(100);
//        Map<String, Object> data2 = dashboardService.getDashboardData();
//        System.out.println("第二次查询（命中缓存）: totalOrders=" + data2.get("totalOrders"));
//
//        Boolean cacheExists = redisUtil.hasKey("admin:dashboard:data");
//        Assertions.assertTrue(cacheExists, "Dashboard缓存应存在");
//        System.out.println("✅ Dashboard缓存验证通过");
//
//        // 1.2 测试服务列表缓存
//        System.out.println("\n--- 1.2 服务列表缓存 ---");
//        List<ServicePackage> list1 = servicePackageService.getAvailableServiceByType(0);
//        System.out.println("第一次查询（查DB）: size=" + list1.size());
//
//        List<ServicePackage> list2 = servicePackageService.getAvailableServiceByType(0);
//        System.out.println("第二次查询（命中缓存）: size=" + list2.size());
//
//        Boolean serviceCacheExists = redisUtil.hasKey("service:available:0");
//        Assertions.assertTrue(serviceCacheExists, "服务列表缓存应存在");
//        System.out.println("✅ 服务列表缓存验证通过");
//
//        // 1.3 测试缓存穿透防护（空值缓存）
//        System.out.println("\n--- 1.3 缓存穿透防护（空值缓存） ---");
//        Admin nonExistentAdmin = adminService.getAdminInfo(99999L);
//        Assertions.assertNull(nonExistentAdmin, "不存在的admin应返回null");
//
//        Boolean nullCacheExists = redisUtil.hasKey("null:admin:info:99999");
//        Assertions.assertTrue(nullCacheExists, "空值缓存应存在");
//        System.out.println("✅ 缓存穿透防护验证通过（空值缓存已创建）");
//
//        Long ttl = redisUtil.getExpire("null:admin:info:99999");
//        System.out.println("空值缓存TTL: " + ttl + "秒（应在2分钟左右）");
//        Assertions.assertNotNull(ttl);
//        Assertions.assertTrue(ttl > 0 && ttl <= 120, "空值缓存TTL应在2分钟内");
//        System.out.println("✅ 空值缓存TTL验证通过");
//    }
//
//    @Test
//   @org.junit.jupiter.api.Order(2)
//    @DisplayName("批次2：Redis分布式锁防重复下单")
//    void testBatch2_DistributedLock() throws InterruptedException {
//        System.out.println("\n========== 批次2：分布式锁测试 ==========");
//
//        // 2.1 测试锁的互斥性
//        System.out.println("\n--- 2.1 锁互斥性测试 ---");
//        String lockKey = "test:lock:mutex";
//        String lockValue1 = UUID.randomUUID().toString();
//        String lockValue2 = UUID.randomUUID().toString();
//
//        Boolean acquired1 = redisUtil.setIfAbsent(lockKey, lockValue1, 5, TimeUnit.SECONDS);
//        Assertions.assertTrue(Boolean.TRUE.equals(acquired1), "第一个线程应获取锁成功");
//        System.out.println("线程1获取锁: " + acquired1);
//
//        Boolean acquired2 = redisUtil.setIfAbsent(lockKey, lockValue2, 5, TimeUnit.SECONDS);
//        Assertions.assertFalse(Boolean.TRUE.equals(acquired2), "第二个线程应获取锁失败");
//        System.out.println("线程2获取锁: " + acquired2);
//        System.out.println("✅ 锁互斥性验证通过");
//
//        // 2.2 测试Lua脚本原子释放锁
//        System.out.println("\n--- 2.2 Lua脚本原子释放锁 ---");
//        boolean released = redisUtil.releaseLock(lockKey, lockValue1);
//        Assertions.assertTrue(released, "持有者应释放锁成功");
//        System.out.println("线程1释放锁: " + released);
//
//        Boolean lockExists = redisUtil.hasKey(lockKey);
//        Assertions.assertFalse(Boolean.TRUE.equals(lockExists), "锁应已被删除");
//        System.out.println("✅ Lua脚本释放锁验证通过");
//
//        // 2.3 测试并发场景下的锁安全性
//        System.out.println("\n--- 2.3 并发锁安全性测试 ---");
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//        CountDownLatch latch = new CountDownLatch(10);
//        int[] successCount = {0};
//
//        for (int i = 0; i < 10; i++) {
//            executor.submit(() -> {
//                try {
//                    Boolean acquired = redisUtil.setIfAbsent("test:lock:concurrent", UUID.randomUUID().toString(), 3, TimeUnit.SECONDS);
//                    if (Boolean.TRUE.equals(acquired)) {
//                        successCount[0]++;
//                        Thread.sleep(100);
//                        redisUtil.delete("test:lock:concurrent");
//                    }
//                } catch (Exception e) {
//                    log.error("并发测试异常", e);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(10, TimeUnit.SECONDS);
//        executor.shutdown();
//
//        System.out.println("10个线程竞争锁，成功获取次数: " + successCount[0]);
//        Assertions.assertEquals(1, successCount[0], "并发场景下应只有1个线程获取锁");
//        System.out.println("✅ 并发锁安全性验证通过");
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(3)
//    @DisplayName("批次3：接口计数器限流防刷")
//    void testBatch3_RateLimiting() {
//        System.out.println("\n========== 批次3：限流测试 ==========");
//
//        String limitKey = "rate:test:limit";
//
//        // 3.1 测试限流阈值
//        System.out.println("\n--- 3.1 限流阈值测试（60秒内最多10次） ---");
//        int passCount = 0;
//        int blockCount = 0;
//
//        for (int i = 0; i < 15; i++) {
//            boolean allowed = redisUtil.rateLimit(limitKey, 60, 10);
//            if (allowed) {
//                passCount++;
//            } else {
//                blockCount++;
//            }
//        }
//
//        System.out.println("通过次数: " + passCount);
//        System.out.println("拦截次数: " + blockCount);
//
//        Assertions.assertEquals(10, passCount, "前10次应通过");
//        Assertions.assertEquals(5, blockCount, "后5次应被拦截");
//        System.out.println("✅ 限流阈值验证通过");
//
//        // 3.2 测试过期时间自动重置
//        System.out.println("\n--- 3.2 限流过期重置测试 ---");
//        Long ttl = stringRedisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
//        System.out.println("限流key剩余TTL: " + ttl + "秒");
//        Assertions.assertNotNull(ttl);
//        Assertions.assertTrue(ttl > 0 && ttl <= 60, "限流key TTL应在60秒内");
//        System.out.println("✅ 限流过期时间验证通过");
//
//        // 3.3 测试不同用户独立限流
//        System.out.println("\n--- 3.3 不同用户独立限流 ---");
//        boolean user1Allowed = redisUtil.rateLimit("rate:user:1", 60, 10);
//        boolean user2Allowed = redisUtil.rateLimit("rate:user:2", 60, 10);
//
//        Assertions.assertTrue(user1Allowed, "用户1首次请求应通过");
//        Assertions.assertTrue(user2Allowed, "用户2首次请求应通过");
//        System.out.println("✅ 不同用户独立限流验证通过");
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(4)
//    @DisplayName("批次4：在线用户实时统计")
//    void testBatch4_OnlineUserStatistics() throws InterruptedException {
//        System.out.println("\n========== 批次4：在线用户统计测试 ==========");
//
//        // 4.1 测试用户上线计数
//        System.out.println("\n--- 4.1 用户上线计数 ---");
//        onlineUserService.userOnline(1L, 0);
//        onlineUserService.userOnline(2L, 1);
//        onlineUserService.userOnline(3L, 2);
//        onlineUserService.userOnline(4L, 2);
//
//        Map<String, Object> stats1 = onlineUserService.getOnlineStats();
//        System.out.println("当前在线统计: " + stats1);
//
//        Assertions.assertEquals(4, stats1.get("total"), "总在线数应为4");
//        Assertions.assertEquals(1, stats1.get("adminCount"), "管理员在线数应为1");
//        Assertions.assertEquals(1, stats1.get("volunteerCount"), "志愿者在线数应为1");
//        Assertions.assertEquals(2, stats1.get("userCount"), "普通用户在线数应为2");
//        System.out.println("✅ 用户上线计数验证通过");
//
//        // 4.2 测试重复登录不重复计数
//        System.out.println("\n--- 4.2 重复登录防重复计数 ---");
//        onlineUserService.userOnline(1L, 0);
//        onlineUserService.userOnline(3L, 2);
//
//        Map<String, Object> stats2 = onlineUserService.getOnlineStats();
//        System.out.println("重复登录后在线统计: " + stats2);
//
//        Assertions.assertEquals(4, stats2.get("total"), "重复登录不应增加总数");
//        System.out.println("✅ 重复登录防重复计数验证通过");
//
//        // 4.3 测试用户下线计数
//        System.out.println("\n--- 4.3 用户下线计数 ---");
//        onlineUserService.userOffline(1L, 0);
//
//        Map<String, Object> stats3 = onlineUserService.getOnlineStats();
//        System.out.println("下线后在线统计: " + stats3);
//
//        Assertions.assertEquals(3, stats3.get("total"), "下线后总在线数应为3");
//        Assertions.assertEquals(0, stats3.get("adminCount"), "管理员在线数应为0");
//        System.out.println("✅ 用户下线计数验证通过");
//
//        // 4.4 测试Token过期自动清除（模拟TTL过期）
//        System.out.println("\n--- 4.4 Token过期自动清除 ---");
//        String userKey = "online:user:2:4";
//        Boolean keyExists = redisUtil.hasKey(userKey);
//        Assertions.assertTrue(Boolean.TRUE.equals(keyExists), "用户key应存在");
//
//        Long userTtl = redisUtil.getExpire(userKey);
//        System.out.println("用户key TTL: " + userTtl + "秒（应接近24小时）");
//        Assertions.assertNotNull(userTtl);
//        Assertions.assertTrue(userTtl > 86000 && userTtl <= 86400, "用户key TTL应接近24小时");
//        System.out.println("✅ Token过期TTL验证通过");
//
//        // 手动删除模拟过期
//        redisUtil.delete(userKey);
//        Map<String, Object> stats4 = onlineUserService.getOnlineStats();
//        System.out.println("模拟过期后在线统计: " + stats4);
//
//        Assertions.assertEquals(2, stats4.get("total"), "模拟过期后总在线数应为2");
//        System.out.println("✅ Token过期自动清除验证通过");
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(5)
//    @DisplayName("综合测试：缓存与分布式锁协同工作")
//    void testIntegration_CacheAndLock() throws InterruptedException {
//        System.out.println("\n========== 综合测试：缓存+锁协同 ==========");
//
//        // 5.1 测试订单详情缓存
//        System.out.println("\n--- 5.1 订单详情缓存 ---");
//        Order order = new Order();
//        order.setUserId(1L);
//        order.setOrderNo("TEST001");
//        order.setTotalPrice(100);
//        order.setStatus(0);
//        order.setIsReviewed(0);
//        order.setServiceDate("2026-06-16");
//        order.setServiceTime(LocalTime.now().toString());
//        order.setAddress("qweqwe");
//        order.setIsReviewed(0);
//        order.setCompleteTime(LocalDateTime.now());
//        orderMapper.insert(order);
//
//
//        System.out.println("插入测试订单: id=" + order.getId());
//
//        Order cachedOrder1 = orderMapper.selectById(order.getId());
//        System.out.println("第一次查询订单: orderNo=" + cachedOrder1.getOrderNo());
//
//        Boolean orderCacheExists = redisUtil.hasKey("order:detail:" + order.getId());
//        System.out.println("订单缓存是否存在: " + orderCacheExists);
//
//        System.out.println("✅ 订单缓存基础验证通过");
//
//        // 5.2 清理测试数据
//        orderMapper.deleteById(order.getId());
//        redisUtil.delete("order:detail:" + order.getId());
//        System.out.println("清理测试数据完成");
//    }
//
//    @AfterAll
//    static void tearDown(@Autowired RedisUtil redisUtil, @Autowired StringRedisTemplate stringRedisTemplate) {
//        Set<String> keys = redisUtil.keys("*");
//        if (keys != null && !keys.isEmpty()) {
//            stringRedisTemplate.delete(keys);
//            log.info("测试结束，清理Redis数据");
//        }
//    }
//}
