package cn.miyf.support;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.LoginUserContext;
import cn.miyf.auth.security.PrincipalType;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;

/**
 * 集成测试基类：共享 PostgreSQL + Redis 容器，本地文件存储。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:33
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("miyf")
                    .withUsername("test")
                    .withPassword("test");

    protected static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        POSTGRES.start();
        REDIS.start();
    }

    /**
     * 注入容器连接信息。
     *
     * @param registry 动态属性
     * @history 1.00 2026-09-05 09:33 XieMingJie Created.
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("PRIMARY_DB_URL", POSTGRES::getJdbcUrl);
        registry.add("PRIMARY_DB_USERNAME", POSTGRES::getUsername);
        registry.add("PRIMARY_DB_PASSWORD", POSTGRES::getPassword);
        registry.add("REDIS_HOST", REDIS::getRedisHost);
        registry.add("REDIS_PORT", () -> String.valueOf(REDIS.getRedisPort()));
        registry.add("JWT_SECRET", () -> "it-test-secret-change-me-to-a-long-random-value");
        registry.add("FILE_STORAGE_TYPE", () -> "local");
        registry.add("app.file-storage.type", () -> "local");
        registry.add("app.redis.rate-limit.enabled", () -> "false");
        registry.add("wx.auth.mock-enabled", () -> "true");
        registry.add("SECONDARY_DB_ENABLED", () -> "false");
    }

    /**
     * 清理登录线程上下文。
     *
     * @history 1.00 2026-09-05 09:33 XieMingJie Created.
     */
    @AfterEach
    void clearLoginContext() {
        LoginUserContext.clear();
    }

    /**
     * 绑定普通用户。
     *
     * @param userId   用户 ID
     * @param username 展示名
     * @history 1.00 2026-09-05 09:33 XieMingJie Created.
     */
    protected void asUser(Long userId, String username) {
        LoginUserContext.set(new AuthPrincipal(userId, username, PrincipalType.USER, List.of(), true));
    }

    /**
     * 绑定管理员（可自定义权限码）。
     *
     * @param adminId     管理员 ID
     * @param permissions 权限码
     * @history 1.00 2026-09-05 09:33 XieMingJie Created.
     */
    protected void asAdmin(Long adminId, String... permissions) {
        LoginUserContext.set(new AuthPrincipal(
                adminId,
                "admin-it",
                PrincipalType.ADMIN,
                Arrays.asList(permissions),
                true));
    }

    /**
     * 绑定具备预约流转所需权限的管理员。
     *
     * @param adminId 管理员 ID
     * @history 1.00 2026-09-05 09:33 XieMingJie Created.
     */
    protected void asOrderAdmin(Long adminId) {
        asAdmin(adminId,
                "kitchen:order:update", "kitchen:order:complete", "kitchen:order:cancel", "kitchen:order:list",
                "kitchen:comment:hide", "kitchen:comment:list", "kitchen:comment:restore", "kitchen:comment:delete",
                "kitchen:dish:list", "kitchen:dish:create", "kitchen:dish:update", "kitchen:dish:publish", "kitchen:dish:unpublish",
                "kitchen:category:list", "kitchen:category:create");
    }
}
