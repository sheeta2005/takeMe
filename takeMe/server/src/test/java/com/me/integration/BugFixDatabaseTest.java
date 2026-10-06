package com.me.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.me.context.BaseContext;
import com.me.controller.volunteer.VolunteerController;
import com.me.dto.OrderTimeoutMessage;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Message;
import com.me.entity.Volunteer;
import com.me.entity.VolunteerLeave;
import com.me.vo.AddressVO;
import com.me.mapper.OrderItemMapper;
import com.me.mq.producer.MessageProducer;
import com.me.redis.aspect.RedisCacheAspect;
import com.me.redis.utils.RedisUtil;
import com.me.service.*;
import com.me.service.Impl.*;
import com.me.service.task.OutboxDispatchTask;
import com.me.util.OssUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 只复制表结构到随机测试库，不读取、清理或修改业务库中的记录。
@EnabledIfSystemProperty(named = "takeme.mysql.tests", matches = "true")
class BugFixDatabaseTest {
    private static final String SERVER_URL = System.getProperty("takeme.test.mysql.url",
            "jdbc:mysql://localhost:3306/?serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    private static final String USER = System.getProperty("takeme.test.mysql.user", "root");
    private static final String PASSWORD = System.getProperty("takeme.test.mysql.password", "");
    private static final String SCHEMA = "takeme_fix_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final List<String> TABLES = List.of("order", "order_item", "volunteer", "user", "admin",
            "service_package", "payment_transaction", "volunteer_points_record", "review",
            "approval", "volunteer_leave", "message", "address", "cart", "cart_item");
    private static AnnotationConfigApplicationContext context;
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();

    @BeforeAll
    static void setup() throws Exception {
        try (Connection connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + SCHEMA + "` CHARACTER SET utf8mb4");
            for (String table : TABLES) {
                statement.execute("CREATE TABLE `" + SCHEMA + "`.`" + table + "` LIKE takeme.`" + table + "`");
            }
        }
        String url = SERVER_URL.replace("3306/", "3306/" + SCHEMA);
        dataSource = new DriverManagerDataSource(url, USER, PASSWORD);
        try (Connection connection = dataSource.getConnection()) {
            var script = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261004_fix_2_10.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, script);
            var businessScript = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261004_business_simplification.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, businessScript);
            ScriptUtils.executeSqlScript(connection, businessScript);
            var performanceScript = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261005_sql_performance.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, performanceScript);
            ScriptUtils.executeSqlScript(connection, performanceScript);
            // 增量迁移必须可重复执行。
            ScriptUtils.executeSqlScript(connection, script);
        }
        jdbc = new JdbcTemplate(dataSource);
        context = new AnnotationConfigApplicationContext();
        // OSS 使用测试替身，仅补充字段注入所需的数值配置，不访问外部存储。
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("aliyun.oss.max-file-size", 0L)));
        context.register(TestConfig.class);
        context.refresh();
    }

    @BeforeEach
    void fixtures() {
        for (String table : TABLES) jdbc.update("DELETE FROM `" + table + "`");
        jdbc.update("DELETE FROM mq_outbox");
        cache.clear();
        RedisUtil redis = context.getBean(RedisUtil.class);
        reset(redis, context.getBean(MessageProducer.class));
        when(redis.cacheMonitor(anyString())).thenReturn(new Object());
        when(redis.get(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
        when(redis.isNullCached(anyString())).thenAnswer(call -> cache.containsKey("null:" + call.getArgument(0)));
        doAnswer(call -> {
            cache.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(redis).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        doAnswer(call -> {
            cache.put("null:" + call.getArgument(0), true);
            return null;
        }).when(redis).setNull(anyString(), anyLong(), any(TimeUnit.class));
        jdbc.update("INSERT INTO volunteer (id, real_name, username, phone, status, points, service_days) "
                + "VALUES (1, '测试志愿者', 'fixture1', 'fixture1', 1, 100, '0')");
        jdbc.update("INSERT INTO volunteer (id, real_name, username, phone, status, points) "
                + "VALUES (2, '测试志愿者2', 'fixture2', 'fixture2', 1, 100)");
        jdbc.update("INSERT INTO `user` (id, username, real_name, phone, status) "
                + "VALUES (1, 'fixture1', '测试老人', 'fixture1', 1), (2, 'fixture2', '测试老人2', 'fixture2', 1)");
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (context != null) context.close();
        // 仅删除本测试创建的随机库，不接受外部指定的库名。
        assertTrue(SCHEMA.startsWith("takeme_fix_test_"));
        try (Connection connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + SCHEMA + "`");
        }
    }

    @Test
    void profileCannotPersistInjectedPoints() {
        Volunteer input = new Volunteer();
        input.setId(2L);
        input.setPoints(999999);
        input.setStatus(0);
        input.setRealName("可编辑姓名");
        BaseContext.setLoginId(1L);
        try {
            context.getBean(VolunteerController.class).update(input);
        } finally {
            BaseContext.clear();
        }
        assertEquals(100, number("SELECT points FROM volunteer WHERE id = 1"));
        assertEquals(1, number("SELECT status FROM volunteer WHERE id = 1"));
        assertEquals("可编辑姓名", text("SELECT real_name FROM volunteer WHERE id = 1"));
    }

    @Test
    void abandonReallyClearsBothDatabaseAssignments() {
        order(10, 11, 1, 1L);
        context.getBean(OrderService.class).volunteerAbandonOrder(1L, 11L);
        assertNull(jdbc.queryForObject("SELECT volunteer_id FROM order_item WHERE id = 11", Long.class));
        assertNull(text("SELECT volunteer_ids FROM `order` WHERE id = 10"));
        assertEquals(0, number("SELECT item_status FROM order_item WHERE id = 11"));
        assertEquals(0, number("SELECT status FROM `order` WHERE id = 10"));
        assertEquals(50, number("SELECT points FROM volunteer WHERE id = 1"));
    }

    @Test
    void cancelReallyClearsAssignedItems() {
        order(10, 11, 1, 1L);
        context.getBean(OrderService.class).cancelOrder(1L, 10L);
        assertNull(jdbc.queryForObject("SELECT volunteer_id FROM order_item WHERE id = 11", Long.class));
        assertNull(text("SELECT volunteer_ids FROM `order` WHERE id = 10"));
        assertEquals(5, number("SELECT status FROM `order` WHERE id = 10"));
        assertEquals(5, number("SELECT item_status FROM order_item WHERE id = 11"));
    }

    @Test
    void releaseVolunteerServicesReallyClearsAssignment() {
        order(10, 11, 1, 1L);
        assertEquals(1, context.getBean(VolunteerService.class).releaseVolunteerServices(1L));
        assertNull(jdbc.queryForObject("SELECT volunteer_id FROM order_item WHERE id = 11", Long.class));
        assertEquals(5, number("SELECT item_status FROM order_item WHERE id = 11"));
        assertNull(text("SELECT volunteer_ids FROM `order` WHERE id = 10"));
        assertEquals(5, number("SELECT status FROM `order` WHERE id = 10"));
        assertEquals(0, context.getBean(VolunteerService.class).releaseVolunteerServices(1L));
    }

    @Test
    void cannotDeleteOtherUsersAddress() {
        jdbc.update("INSERT INTO address (id, user_id, address, is_default) VALUES (1, 2, '他人的地址', 1)");
        BaseContext.setLoginId(1L);
        try {
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> context.getBean(AddressService.class).delete(1L));
        } finally {
            BaseContext.clear();
        }
        assertEquals(1, number("SELECT COUNT(*) FROM address WHERE id = 1 AND user_id = 2"));
    }

    @Test
    void avatarUpdateCannotOverwriteConcurrentPointsAndDeleteWritesNull() {
        jdbc.update("UPDATE volunteer SET avatar = 'old-avatar' WHERE id = 1");
        OssUtil oss = context.getBean(OssUtil.class);
        doAnswer(call -> {
            jdbc.update("UPDATE volunteer SET points = 200 WHERE id = 1");
            return null;
        }).when(oss).deleteFile("old-avatar");
        context.getBean(VolunteerService.class).updateAvatar(1L, "new-avatar");
        assertEquals(200, number("SELECT points FROM volunteer WHERE id = 1"));
        context.getBean(VolunteerService.class).deleteAvatar(1L);
        assertNull(text("SELECT avatar FROM volunteer WHERE id = 1"));
        reset(oss);
    }

    @Test
    void deletingTwoVolunteersDoesNotConflictOnMaskedPhone() {
        assertTrue(context.getBean(VolunteerService.class).logicalDeleteVolunteer(1L));
        assertTrue(context.getBean(VolunteerService.class).logicalDeleteVolunteer(2L));
        assertEquals(2, number("SELECT COUNT(DISTINCT phone) FROM volunteer WHERE status = 0"));
    }

    @Test
    void retryAndConcurrentRetryReturnOneAutoIncrementOrder() throws Exception {
        jdbc.update("INSERT INTO service_package (id, name, type, price, description, status) "
                + "VALUES (1, '测试助餐', 2, 100, '测试套餐', 1)");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Long> create = () -> {
                start.await();
                OrderDTO dto = new OrderDTO();
                dto.setId(999999999999L);
                dto.setRequestId("same-request");
                return context.getBean(OrderService.class).createOrder(1L, dto, List.of(booking())).getId();
            };
            Future<Long> first = workers.submit(create);
            Future<Long> second = workers.submit(create);
            start.countDown();
            Long id = first.get(15, TimeUnit.SECONDS);
            assertEquals(id, second.get(15, TimeUnit.SECONDS));
            assertTrue(id < Integer.MAX_VALUE);
            assertEquals(1, number("SELECT COUNT(*) FROM `order`"));
            assertEquals(1, number("SELECT COUNT(*) FROM order_item"));
            assertEquals(1, number("SELECT COUNT(*) FROM message"));
            assertEquals(0, number("SELECT COUNT(*) FROM mq_outbox"));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void checkoutRetryDoesNotClearNewCartContents() {
        jdbc.update("INSERT INTO service_package (id, name, type, price, description, status) "
                + "VALUES (1, '测试助餐', 2, 100, '测试套餐', 1)");
        jdbc.update("INSERT INTO cart (id, user_id) VALUES (1, 1)");
        insertCartItem();
        CartService service = context.getBean(CartService.class);
        Long orderId = service.checkout(1L, "cart-submit").getId();
        insertCartItem();
        assertEquals(orderId, service.checkout(1L, "cart-submit").getId());
        assertEquals(1, number("SELECT COUNT(*) FROM cart_item"));
        assertEquals(1, number("SELECT COUNT(*) FROM `order`"));
    }

    @Test
    void firstCheckoutForDifferentUsersDoesNotDeadlockOnEmptyRequestIndex() throws Exception {
        jdbc.update("INSERT INTO service_package (id, name, type, price, description, status) "
                + "VALUES (1, '测试助餐', 2, 100, '测试套餐', 1)");
        jdbc.update("INSERT INTO cart (id, user_id) VALUES (1, 1), (2, 2)");
        insertCartItem();
        jdbc.update("INSERT INTO cart_item (cart_id, service_id, service_name, service_price, quantity, "
                + "service_type, service_date, service_time, address) "
                + "VALUES (2, 1, '测试助餐', 100, 1, 2, ?, '10:00', '测试地址')",
                java.time.LocalDate.now().plusDays(2).toString());
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Long> a = workers.submit(() -> { start.await();
                return context.getBean(CartService.class).checkout(1L, "first-submit").getId(); });
            Future<Long> b = workers.submit(() -> { start.await();
                return context.getBean(CartService.class).checkout(2L, "first-submit").getId(); });
            start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(2, number("SELECT COUNT(*) FROM `order`"));
        } finally {
            workers.shutdownNow();
        }
    }

    private void insertCartItem() {
        jdbc.update("INSERT INTO cart_item (cart_id, service_id, service_name, service_price, quantity, "
                + "service_type, service_date, service_time, address) VALUES (1, 1, '测试助餐', 100, 1, 2, ?, '10:00', '测试地址')",
                java.time.LocalDate.now().plusDays(2).toString());
    }

    private OrderItemDTO booking() {
        OrderItemDTO item = new OrderItemDTO();
        item.setServiceId(1L);
        item.setQuantity(1);
        item.setServiceDate(java.time.LocalDate.now().plusDays(2).toString());
        item.setServiceTime("10:00");
        item.setAddress("测试地址");
        return item;
    }

    @Test
    void anotherRoleWithSameIdCannotMarkMessageRead() {
        Message message = notification();
        message.setReceiverType(1);
        context.getBean(MessageService.class).sendMessage(message);
        Long id = jdbc.queryForObject("SELECT id FROM message LIMIT 1", Long.class);
        assertFalse(context.getBean(MessageService.class).markAsRead(id, 2, 1L));
        assertEquals(0, number("SELECT is_read FROM message LIMIT 1"));
        assertTrue(context.getBean(MessageService.class).markAsRead(id, 1, 1L));
    }

    @Test
    void broadcastRetryPreservesReadStateAndTargetsOnlyActiveAccounts() {
        Message template = notification();
        template.setReceiverId(null);
        template.setReceiverType(2);
        context.getBean(MessageService.class).sendMessage(template);
        assertEquals(0, number("SELECT COUNT(*) FROM message"));
        assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox"));
        jdbc.update("UPDATE `user` SET status = 0 WHERE id = 2");
        var raw = new org.springframework.amqp.core.Message(new byte[0],
                new org.springframework.amqp.core.MessageProperties());
        raw.getMessageProperties().setMessageId("broadcast-event");
        var consumer = context.getBean(com.me.mq.consumer.BroadcastNotificationConsumer.class);
        consumer.handle(template, raw);
        jdbc.update("UPDATE message SET is_read = 1");
        consumer.handle(template, raw);
        assertEquals(1, number("SELECT COUNT(*) FROM message"));
        assertEquals(1, number("SELECT is_read FROM message LIMIT 1"));
    }

    @Test
    void defaultAddressOwnershipAndSingleDefaultAreEnforced() throws Exception {
        jdbc.update("INSERT INTO address (id, user_id, address, is_default) "
                + "VALUES (1, 1, '甲', 0), (2, 1, '乙', 0), (3, 2, '丙', 1)");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = workers.submit(() -> setDefault(1L));
            Future<?> b = workers.submit(() -> setDefault(2L));
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
            assertEquals(1, number("SELECT COUNT(*) FROM address WHERE user_id = 1 AND is_default = 1"));
        } finally {
            workers.shutdownNow();
        }
        BaseContext.setLoginId(1L);
        try {
            AddressVO input = new AddressVO();
            input.setId(3L);
            input.setUserId(1L);
            input.setAddress("注入地址");
            input.setIsDefault(1);
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> context.getBean(AddressService.class).update(input));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> context.getBean(AddressService.class).setDefault(3L));
        } finally {
            BaseContext.clear();
        }
        assertEquals(2, number("SELECT user_id FROM address WHERE id = 3"));
        assertEquals("丙", text("SELECT address FROM address WHERE id = 3"));
    }

    private void setDefault(Long id) {
        BaseContext.setLoginId(1L);
        try {
            context.getBean(AddressService.class).setDefault(id);
        } finally {
            BaseContext.clear();
        }
    }

    @Test
    void catalogueMutationInvalidatesOldAndNewTypeCaches() {
        jdbc.update("INSERT INTO service_package (id, name, type, price, description, status) "
                + "VALUES (1, '测试助餐', 2, 100, '测试套餐', 1)");
        var service = context.getBean(ServicePackageService.class);
        service.getAvailableServiceByType(2);
        com.me.entity.ServicePackage changed = new com.me.entity.ServicePackage();
        changed.setId(1L);
        changed.setType(3);
        assertTrue(service.updateById(changed));
        RedisUtil redis = context.getBean(RedisUtil.class);
        verify(redis).delete("service:available:2");
        verify(redis).delete("service:available:3");
        verify(redis, atLeastOnce()).delete("service:available:null");
    }

    @Test
    void accountAvailabilityIsCheckedByRoleAndCurrentStatus() {
        var access = context.getBean(AccountAccessService.class);
        assertTrue(access.isActive(1L, 2));
        assertTrue(access.isActive(1L, 1));
        assertFalse(access.isActive(1L, 0));
        jdbc.update("UPDATE `user` SET status = 0 WHERE id = 1");
        assertFalse(access.isActive(1L, 2));
        assertTrue(access.isActive(1L, 1));
    }

    @Test
    void dashboardUsesMockPaymentLessRefundsNotUnpaidOrderAmounts() {
        order(10, 11, 6, null);
        jdbc.update("INSERT INTO payment_transaction (transaction_no, order_id, order_no, user_id, amount,"
                + " payment_method, payment_status, payment_time) VALUES ('PAY_TEST',10,'TEST10',1,100,'mock',2,NOW())");
        jdbc.update("INSERT INTO payment_transaction (transaction_no, order_id, order_no, user_id, amount,"
                + " payment_method, payment_status, payment_time, refund_time) "
                + "VALUES ('REF_TEST',10,'TEST10',1,40,'mock_refund',2,NOW(),NOW())");
        Map<String, Object> data = context.getBean(AdminDashboardService.class).getDashboardData();
        assertEquals(60L, data.get("todayRevenue"));
        assertEquals(60L, data.get("monthRevenue"));
        assertEquals(1, number("SELECT COUNT(*) FROM `order` WHERE status=6"));
    }

    @Test
    void dashboardCountsPreserveStatusesAndExcludeTomorrowFromToday() {
        order(10, 11, 0, null);
        order(20, 21, 1, 1L);
        order(30, 31, 4, 1L);
        jdbc.update("UPDATE `order` SET create_time=CURDATE()+INTERVAL 1 DAY WHERE id=30");
        Map<String, Object> data = context.getBean(AdminDashboardService.class).getDashboardData();
        assertEquals(3L, data.get("totalOrders"));
        assertEquals(1L, data.get("activeOrders"));
        assertEquals(1L, data.get("pendingOrders"));
        assertEquals(1L, data.get("completedOrders"));
        assertEquals(2L, data.get("todayOrders"));
    }

    @Test
    void paymentSplitRangesUseRefundDateAndIgnoreOtherMethods() {
        order(10, 11, 6, null);
        jdbc.update("INSERT INTO payment_transaction (transaction_no,order_id,order_no,user_id,amount,"
                + "payment_method,payment_status,payment_time,refund_time) VALUES "
                + "('OLD_PAY',10,'TEST10',1,100,'mock',2,CURDATE()-INTERVAL 1 DAY,NULL),"
                + "('NEW_REF',10,'TEST10',1,40,'mock_refund',2,CURDATE()-INTERVAL 1 DAY,NOW()),"
                + "('OTHER',10,'TEST10',1,900,'other',1,NOW(),NULL),"
                + "('PENDING',10,'TEST10',1,800,'mock',0,NOW(),NULL)");
        LocalDateTime start = java.time.LocalDate.now().atStartOfDay();
        assertEquals(-40L, context.getBean(com.me.mapper.PaymentTransactionMapper.class)
                .selectNetMockAmount(start, start.plusDays(1)));
    }

    @Test
    void indexedAppointmentConditionMatchesOldConcatEvenAcrossMidnight() {
        order(10, 11, 0, null);
        order(20, 21, 0, null);
        order(30, 31, 0, null);
        jdbc.update("UPDATE order_item SET service_date='2026-10-05',service_time='22:30' WHERE id=11");
        jdbc.update("UPDATE order_item SET service_date='2026-10-06',service_time='01:30' WHERE id=21");
        jdbc.update("UPDATE order_item SET service_date='2026-10-06',service_time='01:31' WHERE id=31");
        String range = "service_date BETWEEN '2026-10-05' AND '2026-10-06' "
                + "AND (service_date>'2026-10-05' OR service_time>='22:30') "
                + "AND (service_date<'2026-10-06' OR service_time<='01:30')";
        assertEquals(jdbc.queryForList("SELECT id FROM order_item WHERE CONCAT(service_date,' ',service_time)"
                        + " BETWEEN '2026-10-05 22:30' AND '2026-10-06 01:30' ORDER BY id", Long.class),
                jdbc.queryForList("SELECT id FROM order_item WHERE " + range + " ORDER BY id", Long.class));
        assertEquals(2, number("SELECT COUNT(*) FROM order_item WHERE " + range));
    }

    @Test
    void realPaginationKeepsTotalsAndStableOrderWhenCreationTimesTie() {
        order(10, 11, 0, null);
        order(20, 21, 0, null);
        jdbc.update("UPDATE `order` SET create_time=CURDATE()");
        jdbc.update("UPDATE order_item SET create_time=CURDATE()");
        var service = context.getBean(OrderService.class);
        com.me.dto.PageResultDTO page = new com.me.dto.PageResultDTO();
        page.setPageSize(1);
        var first = service.getMyOrderList(1L, null, null, page);
        assertEquals(2, first.getTotal());
        assertEquals(20L, first.getRecords().get(0).getId());
        assertEquals(21L, service.getAvailableOrderList(page).getRecords().get(0).getItems().get(0).getId());
        page.setPageNum(2);
        assertEquals(10L, service.getMyOrderList(1L, null, null, page).getRecords().get(0).getId());
        assertEquals(11L, service.getAvailableOrderList(page).getRecords().get(0).getItems().get(0).getId());
    }

    @Test
    void cachedOldItemCannotCancelServiceStartedByAnotherTransaction() {
        order(10, 11, 1, 1L);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> {
            assertEquals(1, context.getBean(OrderItemMapper.class).selectById(11L).getItemStatus());
            CompletableFuture.runAsync(() -> context.getBean(OrderService.class).userStartService(1L, 11L)).join();
            assertThrows(RuntimeException.class, () -> context.getBean(OrderService.class).cancelOrderItem(1L, 11L));
            status.setRollbackOnly();
        });
        assertEquals(2, number("SELECT item_status FROM order_item WHERE id = 11"));
        assertEquals(2, number("SELECT status FROM `order` WHERE id = 10"));
        assertEquals(0, number("SELECT COUNT(*) FROM payment_transaction"));
    }

    @Test
    void sameVolunteerCanClaimOnlyOneOfTwoOrdersConcurrently() throws Exception {
        order(10, 11, 0, null);
        order(20, 21, 0, null);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = workers.submit(() -> claim(start, 11L));
            Future<Boolean> second = workers.submit(() -> claim(start, 21L));
            start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, number("SELECT COUNT(*) FROM order_item WHERE volunteer_id = 1 AND item_status IN (1,2)"));
        } finally {
            workers.shutdownNow();
        }
    }

    private boolean claim(CountDownLatch start, Long itemId) throws Exception {
        start.await();
        try {
            context.getBean(OrderService.class).volunteerConfirmOrder(1L, itemId);
            return true;
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("正在进行中"), e.toString());
            return false;
        }
    }

    @RepeatedTest(20)
    void twoVolunteersCannotClaimTheSameItem() throws Exception {
        order(10, 11, 0, null);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = workers.submit(() -> competingClaim(start, 1L));
            Future<Boolean> second = workers.submit(() -> competingClaim(start, 2L));
            start.countDown();
            assertEquals(1, (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(15, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, number("SELECT item_status FROM order_item WHERE id = 11"));
            assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox WHERE exchange_name = 'order.status.fanout.exchange'"));
        } finally {
            workers.shutdownNow();
        }
    }

    private boolean competingClaim(CountDownLatch start, Long volunteerId) throws Exception {
        start.await();
        try {
            context.getBean(OrderService.class).volunteerConfirmOrder(volunteerId, 11L);
            return true;
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("已被接取"), e.toString());
            return false;
        }
    }

    @Test
    void adminProfileRefreshesImmediatelyWithoutCache() {
        jdbc.update("INSERT INTO admin (id, username, password, real_name) VALUES (1, 'admin1', 'fixture', '管理员1')");
        jdbc.update("INSERT INTO admin (id, username, password, real_name) VALUES (2, 'admin2', 'fixture', '管理员2')");
        AdminService service = context.getBean(AdminService.class);
        assertEquals(1L, service.getAdminInfo(1L).getId());
        assertNull(service.getAdminInfo(999L));
        assertEquals(2L, service.getAdminInfo(2L).getId());
        assertEquals(1L, service.getAdminInfo(1L).getId());
        jdbc.update("UPDATE admin SET real_name = '新姓名' WHERE id = 1");
        assertEquals("新姓名", service.getAdminInfo(1L).getRealName());
        assertTrue(cache.isEmpty());
    }

    @Test
    void registrationAndDaysApprovalActuallyUpdateVolunteer() {
        jdbc.update("UPDATE volunteer SET status = 0 WHERE id = 1");
        approval(1, "register", 1L, "注册申请");
        ApprovalService service = context.getBean(ApprovalService.class);
        assertTrue(service.approveApplication(1L, null));
        assertEquals(1, number("SELECT status FROM volunteer WHERE id = 1"));
        approval(2, "service_days_change", 1L, "1,3");
        assertTrue(service.approveApplication(2L, "同意"));
        assertEquals("1,3", text("SELECT service_days FROM volunteer WHERE id = 1"));
        assertFalse(service.rejectApplication(2L, "重复审批"));
        assertEquals(2, number("SELECT COUNT(*) FROM mq_outbox"));
    }

    @Test
    void leaveApprovalUpdatesTheLinkedRecordNotTheNewestOne() {
        jdbc.update("INSERT INTO volunteer_leave (id, volunteer_id, type, start_time, end_time, reason) "
                + "VALUES (1, 1, 0, NOW(), NOW(), '第一条'), (2, 1, 0, NOW(), NOW(), '第二条')");
        approval(1, "leave", 1L, "第一条");
        assertTrue(context.getBean(ApprovalService.class).approveApplication(1L, null));
        assertEquals(1, number("SELECT status FROM volunteer_leave WHERE id = 1"));
        assertEquals(0, number("SELECT status FROM volunteer_leave WHERE id = 2"));
    }

    @Test
    void unlinkedHistoricalLeaveIsNotGuessed() {
        approval(1, "leave", null, "历史申请");
        assertThrows(IllegalStateException.class,
                () -> context.getBean(ApprovalService.class).approveApplication(1L, null));
        assertEquals("pending", text("SELECT status FROM approval WHERE id = 1"));
    }

    @Test
    void concurrentOppositeDecisionsProduceOnlyOneBusinessResult() throws Exception {
        jdbc.update("UPDATE volunteer SET status = 0 WHERE id = 1");
        approval(1, "register", 1L, "注册申请");
        ApprovalService service = context.getBean(ApprovalService.class);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> approve = workers.submit(() -> {
                start.await();
                return service.approveApplication(1L, "通过");
            });
            Future<Boolean> reject = workers.submit(() -> {
                start.await();
                return service.rejectApplication(1L, "拒绝");
            });
            start.countDown();
            assertEquals(1, (approve.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (reject.get(15, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox"));
            assertEquals("approved".equals(text("SELECT status FROM approval WHERE id = 1")) ? 1 : 0,
                    number("SELECT status FROM volunteer WHERE id = 1"));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void leaveSubmissionLinksItsExactRecordAndRegistersAnEvent() {
        VolunteerLeave leave = leaveApplication();
        context.getBean(VolunteerLeaveService.class).submit(leave);
        assertEquals(leave.getId(), jdbc.queryForObject("SELECT business_id FROM approval", Long.class));
        assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox"));
        assertTrue(context.getBean(ApprovalService.class).rejectApplication(
                jdbc.queryForObject("SELECT id FROM approval", Long.class), "拒绝"));
        assertEquals(2, number("SELECT status FROM volunteer_leave"));
    }

    @Test
    void leaveSubmissionRollsBackAllRecordsWhenEventRegistrationFails() {
        jdbc.execute("RENAME TABLE mq_outbox TO mq_outbox_unavailable");
        try {
            assertThrows(RuntimeException.class,
                    () -> context.getBean(VolunteerLeaveService.class).submit(leaveApplication()));
            assertEquals(0, number("SELECT COUNT(*) FROM volunteer_leave"));
            assertEquals(0, number("SELECT COUNT(*) FROM approval"));
            assertEquals(0, number("SELECT COUNT(*) FROM message"));
        } finally {
            jdbc.execute("RENAME TABLE mq_outbox_unavailable TO mq_outbox");
        }
    }

    private static VolunteerLeave leaveApplication() {
        VolunteerLeave leave = new VolunteerLeave();
        leave.setVolunteerId(1L);
        leave.setType((byte) 0);
        leave.setStartTime(LocalDateTime.now().plusDays(1).withNano(0));
        leave.setEndTime(LocalDateTime.now().plusDays(2).withNano(0));
        leave.setReason("集成测试请假");
        return leave;
    }

    @Test
    void approvalAndBusinessChangesRollbackWhenEventCannotBeSaved() {
        approval(1, "service_days_change", 1L, "1,3");
        jdbc.execute("RENAME TABLE mq_outbox TO mq_outbox_unavailable");
        try {
            assertThrows(RuntimeException.class,
                    () -> context.getBean(ApprovalService.class).approveApplication(1L, null));
            assertEquals("pending", text("SELECT status FROM approval WHERE id = 1"));
            assertEquals("0", text("SELECT service_days FROM volunteer WHERE id = 1"));
        } finally {
            jdbc.execute("RENAME TABLE mq_outbox_unavailable TO mq_outbox");
        }
    }

    @Test
    void eventRegistrationRollsBackWithBusinessTransaction() {
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE volunteer SET points = 999 WHERE id = 1");
            context.getBean(OutboxService.class).enqueue("order.exchange", "order.create",
                    new OrderTimeoutMessage(10L, "TEST", 1L));
            throw new IllegalStateException("模拟业务回滚");
        }));
        assertEquals(100, number("SELECT points FROM volunteer WHERE id = 1"));
        assertEquals(0, number("SELECT COUNT(*) FROM mq_outbox"));
        verifyNoInteractions(context.getBean(MessageProducer.class));
    }

    @Test
    void failedSendRemainsPendingAndRetryKeepsEventId() {
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> context.getBean(OutboxService.class).enqueue("order.exchange",
                "order.create", new OrderTimeoutMessage(10L, "TEST", 1L)));
        String id = text("SELECT id FROM mq_outbox");
        MessageProducer producer = context.getBean(MessageProducer.class);
        doThrow(new IllegalStateException("模拟 broker 故障")).doNothing()
                .when(producer).sendConfirmed(anyString(), anyString(), any(), eq(id));
        OutboxDispatchTask task = context.getBean(OutboxDispatchTask.class);
        // DATETIME 的秒级精度可能向上取整，测试明确把消息设置为当前到期。
        jdbc.update("UPDATE mq_outbox SET next_attempt_time = NOW()");
        task.dispatchOne(id);
        assertEquals(0, number("SELECT status FROM mq_outbox"));
        assertEquals(1, number("SELECT attempts FROM mq_outbox"));
        assertNotNull(text("SELECT last_error FROM mq_outbox"));
        jdbc.update("UPDATE mq_outbox SET next_attempt_time = NOW()");
        task.dispatchOne(id);
        assertEquals(1, number("SELECT status FROM mq_outbox"));
        assertEquals(2, number("SELECT attempts FROM mq_outbox"));
        assertNull(text("SELECT last_error FROM mq_outbox"));
        verify(producer, times(2)).sendConfirmed(anyString(), anyString(), any(), eq(id));
    }

    @Test
    void duplicateEventDoesNotDuplicateNotificationOrResetReadState() {
        MessageService service = context.getBean(MessageService.class);
        service.sendEventMessage(notification(), "event-1");
        jdbc.update("UPDATE message SET is_read = 1");
        service.sendEventMessage(notification(), "event-1");
        assertEquals(1, number("SELECT COUNT(*) FROM message"));
        assertEquals(1, number("SELECT is_read FROM message"));
    }

    @Test
    void competingDispatchersDoNotSendOnePendingRowTogether() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> context.getBean(OutboxService.class).enqueue(
                "order.exchange", "order.create", new OrderTimeoutMessage(10L, "TEST", 1L)));
        jdbc.update("UPDATE mq_outbox SET next_attempt_time = NOW()");
        String id = text("SELECT id FROM mq_outbox");
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MessageProducer producer = context.getBean(MessageProducer.class);
        doAnswer(call -> {
            sending.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(producer).sendConfirmed(anyString(), anyString(), any(), eq(id));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        OutboxDispatchTask task = context.getBean(OutboxDispatchTask.class);
        try {
            Future<?> first = workers.submit(() -> task.dispatchOne(id));
            assertTrue(sending.await(5, TimeUnit.SECONDS));
            // 第二个发送器必须跳过已被占用的行，不能重复投递或等待第一笔发送。
            workers.submit(() -> task.dispatchOne(id)).get(3, TimeUnit.SECONDS);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertEquals(1, number("SELECT status FROM mq_outbox"));
            verify(producer).sendConfirmed(anyString(), anyString(), any(), eq(id));
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    private static Message notification() {
        Message message = new Message();
        message.setReceiverId(1L);
        message.setReceiverType(2);
        message.setType(1);
        message.setTitle("测试事件");
        message.setContent("测试内容");
        return message;
    }

    private static void approval(int id, String type, Long businessId, String content) {
        jdbc.update("INSERT INTO approval (id, type, applicant_id, business_id, applicant_name, content) "
                + "VALUES (?, ?, 1, ?, '测试志愿者', ?)", id, type, businessId, content);
    }

    private static void order(int orderId, int itemId, int status, Long volunteerId) {
        LocalDateTime appointment = LocalDateTime.now().plusHours(2);
        String date = appointment.toLocalDate().toString();
        String time = appointment.format(DateTimeFormatter.ofPattern("HH:mm"));
        jdbc.update("INSERT INTO `order` (id, order_no, user_id, total_price, service_date, service_time, address, status, volunteer_ids) "
                + "VALUES (?, ?, 1, 100, ?, ?, '测试地址', ?, ?)", orderId, "TEST" + orderId, date, time,
                status, volunteerId == null ? null : volunteerId.toString());
        jdbc.update("INSERT INTO order_item (id, order_id, service_id, service_name, service_price, quantity, item_price, "
                + "service_type, service_date, service_time, address, item_status, volunteer_id) "
                + "VALUES (?, ?, 1, '测试服务', 100, 1, 100, 2, ?, ?, '测试地址', ?, ?)",
                itemId, orderId, date, time, status, volunteerId);
    }

    private static int number(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @MapperScan("com.me.mapper")
    @Import({OrderServiceImpl.class, VolunteerServiceImpl.class, AdminServiceImpl.class,
            ApprovalServiceImpl.class, VolunteerLeaveServiceImpl.class, MessageServiceImpl.class,
            OutboxService.class, OutboxDispatchTask.class, VolunteerController.class, RedisCacheAspect.class,
            AddressServiceImpl.class, CartServiceImpl.class, ServicePackageServiceImpl.class,
            AccountAccessService.class, AdminDashboardServiceImpl.class,
            com.me.mq.consumer.BroadcastNotificationConsumer.class})
    static class TestConfig {
        @Bean DataSource dataSource() { return dataSource; }
        @Bean PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            MybatisConfiguration config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(config);
            // 性能回归必须实际执行 COUNT/LIMIT，不能只把未分页列表装入 Page。
            factory.setPlugins(new com.me.config.MyBatisPlusConfig().mybatisPlusInterceptor());
            return factory.getObject();
        }
        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
        @Bean RedisUtil redisUtil() { return mock(RedisUtil.class); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean OssUtil ossUtil() { return mock(OssUtil.class); }
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
        @Bean MessageProducer messageProducer() { return mock(MessageProducer.class); }
    }
}
