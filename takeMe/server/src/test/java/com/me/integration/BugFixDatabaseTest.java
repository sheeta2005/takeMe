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
import com.me.service.task.MqCleanupTask;
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
    private static final String PASSWORD = System.getProperty("takeme.test.mysql.password",
            System.getenv().getOrDefault("TAKEME_TEST_MYSQL_PASSWORD", ""));
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
            var reviewScript = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261013_review_unique.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, reviewScript);
            ScriptUtils.executeSqlScript(connection, reviewScript);
            var failedMessageScript = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261020_mq_failed_message.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, failedMessageScript);
            ScriptUtils.executeSqlScript(connection, failedMessageScript);
            var retentionScript = new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261021_mq_retention.sql"), "UTF-8");
            ScriptUtils.executeSqlScript(connection, retentionScript);
            ScriptUtils.executeSqlScript(connection, retentionScript);
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

    @Test
    void failedMessageArchiveMigrationCreatesReplayTable() {
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'mq_failed_message'", Integer.class));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = 'mq_failed_message' " +
                "AND index_name = 'idx_failed_status_time'", Integer.class) >= 1);
    }

    @Test
    void failedMessageArchivesDeduplicatesAndReplaysOnlyToOriginalQueue() {
        FailedMessageService service = context.getBean(FailedMessageService.class);
        var properties = new org.springframework.amqp.core.MessageProperties();
        properties.setMessageId("failed-event-1");
        properties.getHeaders().put(com.me.mq.config.RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER,
                "notification.user.queue");
        properties.getHeaders().put("x-exception-message", "测试消费失败");
        properties.getHeaders().put("__TypeId__", "com.me.dto.OrderStatusChangeMessage");
        var original = new org.springframework.amqp.core.Message(new byte[]{0, (byte) 0xff, 1}, properties);

        service.archive(original);
        service.archive(original);
        var page = service.page(1, 20, 0);
        assertEquals(1, page.getTotal());
        var summary = page.getRecords().getFirst();
        assertEquals(2, summary.getOccurrences());
        assertEquals("notification.user.queue", summary.getSourceQueue());
        assertEquals(1, service.recentFailures().size());

        assertTrue(service.replay(summary.getId(), 25L));
        var replay = org.mockito.ArgumentCaptor.forClass(org.springframework.amqp.core.Message.class);
        verify(context.getBean(MessageProducer.class)).replayConfirmed(eq("notification.user.queue"), replay.capture());
        assertArrayEquals(original.getBody(), replay.getValue().getBody());
        assertEquals("failed-event-1", replay.getValue().getMessageProperties().getMessageId());
        assertEquals("com.me.dto.OrderStatusChangeMessage", replay.getValue().getMessageProperties().getHeader("__TypeId__"));
        assertNull(replay.getValue().getMessageProperties().getHeader("x-exception-message"));
        var replayed = service.page(1, 20, 1).getRecords().getFirst();
        assertEquals(1, replayed.getReplayCount());
        assertEquals(25L, replayed.getReplayedBy());
        assertFalse(service.replay(summary.getId(), 25L));

        // 重放后再次失败会重新开放处理，失败发送只保存尝试，不标记成功。
        service.archive(original);
        doThrow(new IllegalStateException("测试队列满"))
                .when(context.getBean(MessageProducer.class)).replayConfirmed(anyString(), any());
        assertFalse(service.replay(summary.getId(), 25L));
        var pending = service.page(1, 20, 0).getRecords().getFirst();
        assertEquals(3, pending.getOccurrences());
        assertEquals(2, pending.getReplayCount());
        assertTrue(pending.getLastReplayError().contains("测试队列满"));
    }

    @Test
    void mqRetentionMigrationPreservesExistingMessagesAndCreatesCleanupIndexes() throws Exception {
        insertOutbox("保留迁移测试", 0, LocalDateTime.now().minusDays(100), LocalDateTime.now());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(
                    new FileSystemResource("../../docs/migrations/20261021_mq_retention.sql"), "UTF-8"));
        }
        assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox"));
        assertEquals(3, number("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                "AND table_name = 'mq_failed_message' AND column_name IN ('closed_by','closed_time','close_reason')"));
        assertEquals(2, number("SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND index_name IN ('idx_outbox_cleanup','idx_failed_cleanup')"));
    }

    @Test
    void outboxCleanupUsesConfirmationTimeAndKeepsPendingAndBoundaryRows() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 1, 0, 0);
        insertOutbox("过期已确认", 1, cutoff.minusDays(20), cutoff.minusSeconds(1));
        insertOutbox("过期待发送", 0, cutoff.minusDays(20), cutoff.minusDays(20));
        insertOutbox("刚刚补发成功", 1, cutoff.minusDays(20), cutoff.plusDays(1));
        insertOutbox("保留期边界", 1, cutoff.minusDays(20), cutoff);
        var mapper = context.getBean(com.me.mapper.OutboxMapper.class);

        assertEquals(1, mapper.deleteConfirmedBefore(cutoff, 500));

        assertEquals(3, number("SELECT COUNT(*) FROM mq_outbox"));
        assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox WHERE status = 0"));
        var summary = mapper.pendingSummary(cutoff);
        assertEquals(1L, ((Number) summary.get("messageCount")).longValue());
        assertEquals(1L, ((Number) summary.get("overdueCount")).longValue());
    }

    @Test
    void cleanupStopsAtBatchBudgetAndDeletesOnlyExpiredClosedFailures() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            insertOutbox("批次" + i, 1, now.minusDays(20), now.minusDays(8));
        }
        insertFailure("过期已结案", 2, now.minusDays(60), now.minusDays(31));
        insertFailure("近期已结案", 2, now.minusDays(60), now.minusDays(1));
        insertFailure("待核查", 0, now.minusDays(60), null);
        insertFailure("仅确认重投", 1, now.minusDays(60), null);
        MqCleanupTask task = new MqCleanupTask(context.getBean(com.me.mapper.OutboxMapper.class),
                context.getBean(com.me.mapper.FailedMessageMapper.class),
                context.getBean(PlatformTransactionManager.class), 7, 30, 2, 2);

        assertEquals(4, task.cleanupOutbox());
        assertEquals(1, number("SELECT COUNT(*) FROM mq_outbox"));
        assertEquals(1, task.cleanupOutbox());
        assertEquals(1, task.cleanupFailedMessages());
        assertEquals(3, number("SELECT COUNT(*) FROM mq_failed_message"));
        assertEquals(2, number("SELECT COUNT(*) FROM mq_failed_message WHERE status IN (0,1)"));
        var outstanding = context.getBean(FailedMessageService.class).outstandingSummary(now.minusHours(24));
        assertEquals(2, outstanding.size());
        for (var row : outstanding) assertEquals(1L, ((Number) row.get("overdueCount")).longValue());
    }

    @Test
    void cleanupCommitsEachBatchAndRollsBackOnlyTheFailingBatch() {
        LocalDateTime old = LocalDateTime.now().minusDays(8);
        for (int i = 0; i < 3; i++) insertOutbox("事务批次" + i, 1, old, old);
        var realMapper = context.getBean(com.me.mapper.OutboxMapper.class);
        var failingMapper = mock(com.me.mapper.OutboxMapper.class);
        var batches = new java.util.concurrent.atomic.AtomicInteger();
        when(failingMapper.deleteConfirmedBefore(any(), anyInt())).thenAnswer(call -> {
            int deleted = realMapper.deleteConfirmedBefore(call.getArgument(0), call.getArgument(1));
            if (batches.incrementAndGet() == 2) throw new IllegalStateException("模拟删除后事务失败");
            return deleted;
        });
        var manager = context.getBean(PlatformTransactionManager.class);
        MqCleanupTask task = new MqCleanupTask(failingMapper,
                context.getBean(com.me.mapper.FailedMessageMapper.class), manager, 7, 30, 1, 10);
        // 外层事务回滚不撤销首批提交，第二批删除则必须随自身事务回滚。
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            assertEquals(1, task.cleanupOutbox());
            throw new IllegalStateException("模拟外层回滚");
        }));
        assertEquals(2, number("SELECT COUNT(*) FROM mq_outbox"));
        assertEquals(2, batches.get());
    }

    @Test
    void closedFailureCanBeFilteredAndReopensOnAnotherConsumerFailure() {
        FailedMessageService service = context.getBean(FailedMessageService.class);
        var properties = new org.springframework.amqp.core.MessageProperties();
        properties.setMessageId("再次失败");
        properties.getHeaders().put(com.me.mq.config.RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER,
                "notification.user.queue");
        var message = new org.springframework.amqp.core.Message(new byte[]{1}, properties);
        service.archive(message);
        String id = service.page(1, 20, 0).getRecords().getFirst().getId();
        assertTrue(service.replay(id, 25L));
        assertTrue(service.close(id, 25L, " 已核查业务恢复 "));
        var closed = service.page(1, 20, 2).getRecords().getFirst();
        assertEquals("已核查业务恢复", closed.getCloseReason());
        assertEquals(25L, closed.getClosedBy());
        assertNotNull(closed.getClosedTime());
        assertFalse(service.replay(id, 25L));
        assertFalse(service.close(id, 25L, "重复结案"));

        service.archive(message);

        var reopened = service.page(1, 20, 0).getRecords().getFirst();
        assertEquals(2, reopened.getOccurrences());
        assertNull(reopened.getClosedBy());
        assertNull(reopened.getClosedTime());
        assertNull(reopened.getCloseReason());
        assertEquals(0, service.page(1, 20, 2).getTotal());
    }

    @Test
    void cleanupRechecksStatusWhenConcurrentArchiveReopensLockedFailure() throws Exception {
        FailedMessageService service = context.getBean(FailedMessageService.class);
        var properties = new org.springframework.amqp.core.MessageProperties();
        properties.setMessageId("并发归档");
        properties.getHeaders().put(com.me.mq.config.RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER,
                "notification.user.queue");
        var message = new org.springframework.amqp.core.Message(new byte[]{1}, properties);
        service.archive(message);
        String id = service.page(1, 20, 0).getRecords().getFirst().getId();
        service.close(id, 25L, "原故障已恢复");
        jdbc.update("UPDATE mq_failed_message SET closed_time = ? WHERE id = ?", LocalDateTime.now().minusDays(40), id);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        var deletion = new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
        try {
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                service.archive(message);
                deletion.set(executor.submit(() -> {
                    started.countDown();
                    return context.getBean(com.me.mapper.FailedMessageMapper.class)
                            .deleteClosedBefore(LocalDateTime.now().minusDays(30), 500);
                }));
                try {
                    assertTrue(started.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> deletion.get().get(200, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            });
            assertEquals(0, deletion.get().get(5, TimeUnit.SECONDS));
            assertEquals(0, number("SELECT status FROM mq_failed_message"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void replayAndCloseSerializeWithoutOverwritingTheFinalClosedState() throws Exception {
        String id = insertFailure("并发重投结案", 0, LocalDateTime.now(), null);
        FailedMessageService service = context.getBean(FailedMessageService.class);
        CountDownLatch publishing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            publishing.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(context.getBean(MessageProducer.class)).replayConfirmed(anyString(), any());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> replay = executor.submit(() -> service.replay(id, 25L));
            assertTrue(publishing.await(5, TimeUnit.SECONDS));
            Future<Boolean> close = executor.submit(() -> service.close(id, 25L, "已核查恢复"));
            assertThrows(TimeoutException.class, () -> close.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(replay.get(5, TimeUnit.SECONDS));
            assertTrue(close.get(5, TimeUnit.SECONDS));
            assertEquals(2, number("SELECT status FROM mq_failed_message"));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    // 直接准备生命周期边界数据，不依赖外部 Broker。
    private void insertOutbox(String id, int status, LocalDateTime created, LocalDateTime updated) {
        jdbc.update("INSERT INTO mq_outbox (id, exchange_name, routing_key, payload_type, payload, status, " +
                        "attempts, next_attempt_time, create_time, update_time) VALUES (?, '', '', '', '{}', ?, 0, ?, ?, ?)",
                id, status, updated, created, updated);
    }

    private String insertFailure(String label, int status, LocalDateTime failureTime, LocalDateTime closedTime) {
        // 归档主键为 ASCII 摘要，中文标签仅用于生成稳定的测试标识。
        String id = UUID.nameUUIDFromBytes(label.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        jdbc.update("INSERT INTO mq_failed_message (id, message_id, source_queue, exchange_name, routing_key, body, " +
                        "headers_json, reason, status, create_time, last_failure_time, closed_time) " +
                        "VALUES (?, ?, 'notification.user.queue', '', '', ?, '{}', 'consumer-failure', ?, ?, ?, ?)",
                id, label, new byte[]{1}, status, failureTime, failureTime, closedTime);
        return id;
    }

    @BeforeEach
    void fixtures() {
        for (String table : TABLES) jdbc.update("DELETE FROM `" + table + "`");
        jdbc.update("DELETE FROM mq_outbox");
        jdbc.update("DELETE FROM mq_failed_message");
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
        // 接单用例按当天排班，避免测试结果随星期变化。
        jdbc.update("UPDATE volunteer SET service_days = ?",
                String.valueOf(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).getDayOfWeek().getValue() % 7));
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

    // 用真实数据库验证批准请假的过滤、解绑、消息与事务边界。
    @Test
    void approvedFutureLeaveFiltersListAndRejectsClaimAtBoundary() {
        order(10, 11, 0, null);
        LocalDateTime appointment = jdbc.queryForObject(
                "SELECT TIMESTAMP(service_date, service_time) FROM order_item WHERE id = 11", LocalDateTime.class);
        leave(1, appointment, appointment, 1);
        OrderService service = context.getBean(OrderService.class);
        assertEquals(1, context.getBean(VolunteerService.class).getWorkStatus(
                context.getBean(VolunteerService.class).getById(1L)));
        assertEquals(0, service.getAvailableOrderList(1L, new com.me.dto.PageResultDTO()).getTotal());
        assertEquals(1, service.getAvailableOrderList(2L, new com.me.dto.PageResultDTO()).getTotal());
        assertTrue(assertThrows(RuntimeException.class, () -> service.volunteerConfirmOrder(1L, 11L))
                .getMessage().contains("请假冲突"));
        assertNull(jdbc.queryForObject("SELECT volunteer_id FROM order_item WHERE id = 11", Long.class));
    }

    @Test
    void pendingAndRejectedLeaveDoNotPreventClaim() {
        order(10, 11, 0, null);
        leave(1, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(4), 0);
        leave(2, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(4), 2);
        context.getBean(OrderService.class).volunteerConfirmOrder(1L, 11L);
        assertEquals(1, number("SELECT volunteer_id FROM order_item WHERE id = 11"));
    }

    @Test
    void currentLeaveAndRestPreventClaimButKeepExistingTodosReadable() {
        order(10, 11, 0, null);
        leave(1, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), 1);
        VolunteerService volunteers = context.getBean(VolunteerService.class);
        OrderService orders = context.getBean(OrderService.class);
        assertEquals(3, volunteers.getWorkStatus(volunteers.getById(1L)));
        assertTrue(assertThrows(RuntimeException.class, () -> orders.volunteerConfirmOrder(1L, 11L))
                .getMessage().contains("请假状态"));
        order(20, 21, 1, 1L);
        assertEquals(1, orders.getVolunteerOrderList(1L, null, null, new com.me.dto.PageResultDTO()).getTotal());
        jdbc.update("DELETE FROM volunteer_leave");
        jdbc.update("UPDATE volunteer SET service_days = NULL WHERE id = 1");
        assertEquals(0, volunteers.getWorkStatus(volunteers.getById(1L)));
        assertEquals(1, orders.getVolunteerOrderList(1L, null, null, new com.me.dto.PageResultDTO()).getTotal());
        jdbc.update("UPDATE order_item SET item_status = 3 WHERE id = 21");
        assertTrue(assertThrows(RuntimeException.class, () -> orders.volunteerConfirmOrder(1L, 11L))
                .getMessage().contains("不是您的工作日"));
    }

    @Test
    void leaveApprovalReopensOnlyConflictingServicesAndNotifiesWithoutPenalty() {
        order(10, 11, 1, 1L);
        order(20, 21, 2, 1L);
        order(30, 31, 1, 1L);
        jdbc.update("UPDATE order_item SET service_date = DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 2 DAY), '%Y-%m-%d') WHERE id = 31");
        leave(1, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 0);
        approval(1, "leave", 1L, "请假");
        ApprovalService service = context.getBean(ApprovalService.class);
        assertTrue(service.approveApplication(1L, "同意"));
        assertEquals(2, number("SELECT COUNT(*) FROM order_item WHERE id IN (11,21) AND volunteer_id IS NULL AND item_status = 0"));
        assertEquals(2, number("SELECT COUNT(*) FROM `order` WHERE id IN (10,20) AND volunteer_ids IS NULL AND status = 0"));
        assertEquals(1, number("SELECT volunteer_id FROM order_item WHERE id = 31"));
        assertEquals(100, number("SELECT points FROM volunteer WHERE id = 1"));
        assertEquals(0, number("SELECT COUNT(*) FROM volunteer_points_record"));
        assertEquals(2, number("SELECT COUNT(*) FROM message WHERE receiver_id = 1 AND receiver_type = 1 AND title = '请假批准，冲突服务已释放'"));
        assertFalse(service.approveApplication(1L, "重复"));
        assertEquals(2, number("SELECT COUNT(*) FROM message WHERE title = '请假批准，冲突服务已释放'"));
    }

    @Test
    void rejectedLeaveKeepsAssignedService() {
        order(10, 11, 1, 1L);
        leave(1, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 0);
        approval(1, "leave", 1L, "请假");
        assertTrue(context.getBean(ApprovalService.class).rejectApplication(1L, "拒绝"));
        assertEquals(1, number("SELECT volunteer_id FROM order_item WHERE id = 11"));
        assertEquals(0, number("SELECT COUNT(*) FROM message WHERE title = '请假批准，冲突服务已释放'"));
    }

    @Test
    void failedLeaveNotificationRollsBackReleaseAndApprovalTogether() {
        order(10, 11, 1, 1L);
        leave(1, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 0);
        approval(1, "leave", 1L, "请假");
        jdbc.execute("RENAME TABLE mq_outbox TO mq_outbox_unavailable");
        try {
            assertThrows(RuntimeException.class,
                    () -> context.getBean(ApprovalService.class).approveApplication(1L, "同意"));
            assertEquals(1, number("SELECT volunteer_id FROM order_item WHERE id = 11"));
            assertEquals(1, number("SELECT item_status FROM order_item WHERE id = 11"));
            assertEquals("1", text("SELECT volunteer_ids FROM `order` WHERE id = 10"));
            assertEquals(0, number("SELECT status FROM volunteer_leave WHERE id = 1"));
            assertEquals("pending", text("SELECT status FROM approval WHERE id = 1"));
            assertEquals(0, number("SELECT COUNT(*) FROM message"));
        } finally {
            jdbc.execute("RENAME TABLE mq_outbox_unavailable TO mq_outbox");
        }
    }

    @RepeatedTest(10)
    void concurrentClaimAndLeaveApprovalNeverRetainConflictingAssignment() throws Exception {
        order(10, 11, 0, null);
        leave(1, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 0);
        approval(1, "leave", 1L, "请假");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> claim = workers.submit(() -> {
                start.await();
                try {
                    context.getBean(OrderService.class).volunteerConfirmOrder(1L, 11L);
                } catch (com.me.exception.OrderBusinessException expected) {
                    assertTrue(expected.getMessage().contains("请假冲突"));
                }
                return null;
            });
            Future<Boolean> approve = workers.submit(() -> {
                start.await();
                return context.getBean(ApprovalService.class).approveApplication(1L, "同意");
            });
            start.countDown();
            claim.get(15, TimeUnit.SECONDS);
            assertTrue(approve.get(15, TimeUnit.SECONDS));
            assertNull(jdbc.queryForObject("SELECT volunteer_id FROM order_item WHERE id = 11", Long.class));
            assertEquals(0, number("SELECT item_status FROM order_item WHERE id = 11"));
            assertEquals(1, number("SELECT status FROM volunteer_leave WHERE id = 1"));
        } finally {
            workers.shutdownNow();
        }
    }

    private static void leave(int id, LocalDateTime start, LocalDateTime end, int status) {
        jdbc.update("INSERT INTO volunteer_leave (id, volunteer_id, type, start_time, end_time, reason, status) "
                + "VALUES (?, 1, 0, ?, ?, '测试请假', ?)", id, start, end, status);
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
        String originalDays = text("SELECT service_days FROM volunteer WHERE id = 1");
        approval(1, "service_days_change", 1L, "1,3");
        jdbc.execute("RENAME TABLE mq_outbox TO mq_outbox_unavailable");
        try {
            assertThrows(RuntimeException.class,
                    () -> context.getBean(ApprovalService.class).approveApplication(1L, null));
            assertEquals("pending", text("SELECT status FROM approval WHERE id = 1"));
            assertEquals(originalDays, text("SELECT service_days FROM volunteer WHERE id = 1"));
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

    // 评价回归覆盖服务项独立性、回显、接口参数及人工/默认评价之间的真实数据库竞争。
    @Test
    void itemEvaluationEndpointReturnsSavedReviewAndEachServiceCanBeEditedIndependently() throws Exception {
        order(10, 11, 4, 1L);
        order(20, 21, 4, 2L);
        jdbc.update("UPDATE order_item SET order_id = 10, service_name = '助洁' WHERE id = 21");
        jdbc.update("UPDATE order_item SET service_name = '助餐' WHERE id = 11");
        jdbc.update("DELETE FROM `order` WHERE id = 20");
        OrderService service = context.getBean(OrderService.class);
        assertTrue(service.getOrderDetail(1L, 10L).getItems().stream().noneMatch(item -> item.getReviewed()));
        com.me.utils.JwtUtil jwt = mock(com.me.utils.JwtUtil.class);
        when(jwt.getUserIdFromAuthHeader("Bearer test-review")).thenReturn(1L);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.me.controller.user.OrderController(service, jwt)).build();
        BaseContext.setLoginId(1L);
        try {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/user/order/evaluateItem").header("Authorization", "Bearer test-review").param("orderItemId", "11")
                            .param("rating", "2").param("comment", "助餐评价原文"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
        } finally {
            BaseContext.clear();
        }
        var reviewed = service.getOrderDetail(1L, 10L).getItems().stream()
                .filter(item -> item.getId() == 11L).findFirst().orElseThrow();
        assertTrue(reviewed.getReviewed());
        assertEquals(2, reviewed.getReview().getRating());
        assertEquals("助餐评价原文", reviewed.getReview().getComment());
        Long reviewId = reviewed.getReview().getId();
        service.evaluateOrderItem(1L, 11L, 4, "修改后的助餐评价");
        service.evaluateOrderItem(1L, 21L, 1, "独立的助洁评价");
        var detail = service.getOrderDetail(1L, 10L);
        assertEquals(2, number("SELECT COUNT(*) FROM review WHERE order_id = 10"));
        assertEquals(reviewId, jdbc.queryForObject("SELECT id FROM review WHERE order_item_id = 11", Long.class));
        assertEquals("修改后的助餐评价", text("SELECT comment FROM review WHERE order_item_id = 11"));
        assertEquals(1, number("SELECT rating FROM review WHERE order_item_id = 21"));
        assertTrue(detail.getItems().stream().allMatch(item -> item.getReviewed()));
        assertTrue(service.getMyOrderList(1L, null, null, new com.me.dto.PageResultDTO())
                .getRecords().get(0).getItems().stream().allMatch(item -> item.getReviewed()));
    }

    @Test
    void cannotReviewAnotherUsersOrUnfinishedService() {
        order(10, 11, 1, 1L);
        OrderService service = context.getBean(OrderService.class);
        assertThrows(com.me.exception.OrderBusinessException.class,
                () -> service.evaluateOrderItem(2L, 11L, 5, "他人的订单"));
        assertThrows(com.me.exception.OrderBusinessException.class,
                () -> service.evaluateOrderItem(1L, 11L, 5, "尚未履约"));
        assertEquals(0, number("SELECT COUNT(*) FROM review"));
    }

    @Test
    void defaultReviewOnlyFillsEligibleMissingRecordAndCannotOverwriteManualReview() {
        order(10, 11, 4, 1L);
        OrderService service = context.getBean(OrderService.class);
        jdbc.update("UPDATE `order` SET complete_time = NOW() WHERE id = 10");
        assertFalse(service.autoEvaluateOrderItem(11L));
        jdbc.update("UPDATE `order` SET complete_time = DATE_SUB(NOW(), INTERVAL 4 DAY) WHERE id = 10");
        assertTrue(service.autoEvaluateOrderItem(11L));
        assertFalse(service.autoEvaluateOrderItem(11L));
        service.evaluateOrderItem(1L, 11L, 2, "人工评价优先");
        context.getBean(com.me.task.AutoReviewTask.class).autoEvaluateCompletedOrders();
        assertEquals(1, number("SELECT COUNT(*) FROM review WHERE order_item_id = 11"));
        assertEquals(2, number("SELECT rating FROM review WHERE order_item_id = 11"));
        assertEquals("人工评价优先", text("SELECT comment FROM review WHERE order_item_id = 11"));
    }

    @RepeatedTest(10)
    void concurrentManualSubmissionsAlwaysShareOneReviewRecord() throws Exception {
        order(10, 11, 4, 1L);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = workers.submit(() -> {
                start.await();
                context.getBean(OrderService.class).evaluateOrderItem(1L, 11L, 2, "评价甲");
                return null;
            });
            Future<?> second = workers.submit(() -> {
                start.await();
                context.getBean(OrderService.class).evaluateOrderItem(1L, 11L, 4, "评价乙");
                return null;
            });
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertEquals(1, number("SELECT COUNT(*) FROM review WHERE order_item_id = 11"));
            int rating = number("SELECT rating FROM review WHERE order_item_id = 11");
            assertEquals(rating == 2 ? "评价甲" : "评价乙", text("SELECT comment FROM review WHERE order_item_id = 11"));
            assertEquals(1, number("SELECT COUNT(*) FROM message WHERE title = '收到新评价'"));
        } finally {
            workers.shutdownNow();
        }
    }

    @RepeatedTest(10)
    void concurrentDefaultTasksAndManualReviewAlwaysKeepManualContent() throws Exception {
        order(10, 11, 4, 1L);
        jdbc.update("UPDATE `order` SET complete_time = DATE_SUB(NOW(), INTERVAL 4 DAY) WHERE id = 10");
        ExecutorService workers = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> manual = workers.submit(() -> {
                start.await();
                context.getBean(OrderService.class).evaluateOrderItem(1L, 11L, 3, "用户手动提交的评价");
                return null;
            });
            List<Future<?>> defaults = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                defaults.add(workers.submit(() -> {
                    start.await();
                    context.getBean(com.me.task.AutoReviewTask.class).autoEvaluateCompletedOrders();
                    return null;
                }));
            }
            start.countDown();
            manual.get(15, TimeUnit.SECONDS);
            for (Future<?> task : defaults) task.get(15, TimeUnit.SECONDS);
            assertEquals(1, number("SELECT COUNT(*) FROM review WHERE order_item_id = 11"));
            assertEquals(3, number("SELECT rating FROM review WHERE order_item_id = 11"));
            assertEquals("用户手动提交的评价", text("SELECT comment FROM review WHERE order_item_id = 11"));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void uniqueConstraintRejectsDuplicateAndMigrationDoesNotDeleteHistory() throws Exception {
        order(10, 11, 4, 1L);
        context.getBean(OrderService.class).evaluateOrderItem(1L, 11L, 5, "第一条历史评价");
        String insert = "INSERT INTO review (order_id, order_item_id, user_id, volunteer_id, rating, comment) "
                + "VALUES (10, 11, 1, 1, 1, '第二条历史评价')";
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(insert));
        jdbc.execute("ALTER TABLE review DROP INDEX uk_review_order_item");
        try {
            jdbc.update(insert);
            try (Connection connection = dataSource.getConnection()) {
                assertThrows(org.springframework.jdbc.datasource.init.ScriptException.class,
                        () -> ScriptUtils.executeSqlScript(connection, new EncodedResource(
                                new FileSystemResource("../../docs/migrations/20261013_review_unique.sql"), "UTF-8")));
            }
            assertEquals(2, number("SELECT COUNT(*) FROM review WHERE order_item_id = 11"));
        } finally {
            // 仅恢复随机测试库结构；业务库的历史重复数据不会被本测试改动。
            jdbc.update("DELETE FROM review");
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new FileSystemResource("../../docs/migrations/20261013_review_unique.sql"), "UTF-8"));
            }
        }
    }

    @Test
    void reviewAndNotificationRollbackTogetherWhenMessageSaveFails() {
        order(10, 11, 4, 1L);
        jdbc.execute("RENAME TABLE message TO message_unavailable");
        try {
            assertThrows(RuntimeException.class,
                    () -> context.getBean(OrderService.class).evaluateOrderItem(1L, 11L, 4, "评价"));
            assertEquals(0, number("SELECT COUNT(*) FROM review"));
            assertEquals(0, number("SELECT COUNT(*) FROM message_unavailable"));
        } finally {
            jdbc.execute("RENAME TABLE message_unavailable TO message");
        }
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
    @Import({OrderServiceImpl.class, VolunteerServiceImpl.class, VolunteerAvailabilityService.class, AdminServiceImpl.class,
            ApprovalServiceImpl.class, VolunteerLeaveServiceImpl.class, MessageServiceImpl.class, com.me.task.AutoReviewTask.class,
            OutboxService.class, OutboxDispatchTask.class, FailedMessageService.class, VolunteerController.class, RedisCacheAspect.class,
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
