package io.authplatform.platform.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers.
 *
 * <p>This class provides a shared PostgreSQL container for all integration tests,
 * ensuring tests run against a real database with proper schema and migrations.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Automatic PostgreSQL container lifecycle management</li>
 *   <li>Flyway migrations applied automatically</li>
 *   <li>Container reused across test classes for performance</li>
 *   <li>Isolated test data (each test runs in its own transaction)</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * @DataJpaTest
 * @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
 * class MyRepositoryIntegrationTest extends BaseIntegrationTest {
 *     @Autowired
 *     private MyRepository repository;
 *
 *     @Test
 *     void shouldTestRepositoryOperation() {
 *         // Test code here
 *     }
 * }
 * }</pre>
 */
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * Shared PostgreSQL 15 container for all integration tests.
     *
     * <p>Using a single static container improves test performance by avoiding
     * container startup overhead for each test class. The container is
     * automatically started before the first test and stopped after all tests complete.
     *
     * <p>This container is started manually in a static initializer block to ensure
     * it's shared across all test classes and only started once.
     */
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("authplatform_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        POSTGRES_CONTAINER.start();
    }

    /**
     * Dynamically configure Spring datasource properties from the Testcontainer.
     *
     * <p>This method is called before the Spring context is created, allowing
     * us to inject the dynamically assigned container connection details.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        // Ensure Flyway runs migrations
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-on-validation-error", () -> "true");
    }

    /**
     * Setup method run before each test.
     *
     * <p>Subclasses can override this method to add custom setup logic:
     * <pre>{@code
     * @BeforeEach
     * @Override
     * void setUp() {
     *     super.setUp();
     *     // Custom setup
     * }
     * }</pre>
     */
    @BeforeEach
    void setUp() {
        // Can be overridden by subclasses for additional setup
    }
}
