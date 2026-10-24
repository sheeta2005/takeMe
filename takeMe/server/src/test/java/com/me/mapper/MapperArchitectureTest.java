package com.me.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MapperArchitectureTest {
    private static Configuration configuration;
    private static final Pattern BUSINESS_QUERY_IMPLEMENTATION = Pattern.compile(
            "\\bcom\\.baomidou\\.mybatisplus\\.core\\.(?:conditions|toolkit\\.Wrappers)\\b"
                    + "|\\b(?:LambdaQueryWrapper|LambdaUpdateWrapper|QueryWrapper|UpdateWrapper|Wrapper|Wrappers|"
                    + "LambdaQueryChainWrapper|LambdaUpdateChainWrapper|QueryChainWrapper|UpdateChainWrapper)\\b"
                    + "|\\.\\s*(?:lambdaQuery|lambdaUpdate|query|update)\\s*\\(\\s*\\)");
    private static final Pattern COMPLEX_MP_OPERATION = Pattern.compile(
            "\\.\\s*(?:apply|inSql|notInSql|exists|notExists|eqSql|gtSql|geSql|ltSql|leSql|setSql|"
                    + "select|selectCount|selectMaps|selectMapsPage|selectObjs|groupBy|having|last)\\s*\\(");

    @BeforeAll
    static void loadXmlMappings() throws Exception {
        // 只解析映射，不连接数据库；缺失或重复 statement 必须在普通测试中暴露。
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(new DriverManagerDataSource("jdbc:mysql://localhost:3306/takeme_mapping_test"));
        MybatisConfiguration config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(config);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        configuration = factory.getObject().getConfiguration();
        configuration.addMappers("com.me.mapper");
    }

    @Test
    void customStatementsAreLoadedFromXml() {
        for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) {
            for (var method : mapper.getDeclaredMethods()) {
                if (method.isDefault() || Modifier.isStatic(method.getModifiers()) || method.isSynthetic()) continue;
                String id = mapper.getName() + "." + method.getName();
                assertTrue(configuration.hasStatement(id, false), id);
                assertTrue(configuration.getMappedStatement(id).getResource().replace('\\', '/').contains("mapper/"), id);
            }
        }
    }

    @Test
    void mapperMethodsDoNotDeclareSqlAnnotations() {
        // Mapper 和 Param 是接口元数据；语句及执行选项统一由 XML 承担。
        for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) {
            for (var method : mapper.getDeclaredMethods()) {
                for (var annotation : method.getDeclaredAnnotations()) {
                    assertFalse(annotation.annotationType().getPackageName().equals("org.apache.ibatis.annotations"),
                            mapper.getName() + "." + method.getName());
                }
            }
        }
    }

    @Test
    void queryConditionsStayInsideMappers() throws Exception {
        // 导入、全限定类名和链式查询都不能绕过分层约束；保留普通 CRUD 委托。
        try (var paths = Files.walk(Path.of("src/main/java/com/me"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (path.getParent().getFileName().toString().equals("mapper")) continue;
                assertFalse(BUSINESS_QUERY_IMPLEMENTATION.matcher(sourceWithoutComments(path)).find(), path.toString());
            }
        }
    }

    @Test
    void mapperImplementationsOnlyUseSimpleMpOperations() throws Exception {
        // 聚合、固定投影和 SQL 片段统一回到 XML；普通查询的固定 LIMIT 保持原语义。
        try (var paths = Files.list(Path.of("src/main/java/com/me/mapper"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = sourceWithoutComments(path)
                        .replaceAll("\\.\\s*last\\s*\\(\\s*\"(?i:LIMIT)\\s+\\d+\"\\s*\\)", "");
                assertFalse(COMPLEX_MP_OPERATION.matcher(source).find(), path.toString());
            }
        }
    }

    @Test
    void queryGuardCoversFullyQualifiedAndChainImplementations() {
        // 回归此前仅检查 import 时无法拦截的两种写法。
        assertTrue(BUSINESS_QUERY_IMPLEMENTATION.matcher(
                "new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()").find());
        assertTrue(BUSINESS_QUERY_IMPLEMENTATION.matcher("this.lambdaQuery().eq(User::getStatus, 1)").find());
        assertTrue(BUSINESS_QUERY_IMPLEMENTATION.matcher("this.update().set(User::getStatus, 1)").find());
        assertFalse(BUSINESS_QUERY_IMPLEMENTATION.matcher("baseMapper.updateById(user)").find());
    }

    private static String sourceWithoutComments(Path path) throws Exception {
        // 忽略历史注释中的示例，仅约束当前生效的实现。
        return Files.readString(path).replaceAll("(?s)/\\*.*?\\*/|(?m)^\\s*//.*$", "");
    }

    @Test
    void currentReadsRetainCacheControls() {
        // 包括不加行锁但原本要求刷新缓存的志愿者活动项查询。
        List<String> ids = List.of("ApprovalMapper.selectForUpdate", "FailedMessageMapper.selectForReplay",
                "OrderItemMapper.selectForUpdate", "OrderItemMapper.selectByOrderForUpdate",
                "OrderItemMapper.selectActiveForVolunteer", "OrderMapper.selectByRequestForUpdate",
                "OrderMapper.selectForUpdate", "OrderMapper.selectByItemForUpdate", "OutboxMapper.selectDueForUpdate",
                "ReviewMapper.selectByItemForUpdate", "UserMapper.selectForUpdate",
                "VolunteerLeaveMapper.selectForUpdate", "VolunteerMapper.selectForUpdate");
        for (String id : ids) {
            MappedStatement statement = configuration.getMappedStatement("com.me.mapper." + id);
            assertFalse(statement.isUseCache(), id);
            assertTrue(statement.isFlushCacheRequired(), id);
        }
    }
}
