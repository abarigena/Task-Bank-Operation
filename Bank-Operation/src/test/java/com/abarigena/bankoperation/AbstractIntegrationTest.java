package com.abarigena.bankoperation;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Абстрактный базовый класс для интеграционных тестов с использованием Testcontainers.
 * Запускает контейнеры PostgreSQL и Cassandra перед выполнением тестов в классах-наследниках.
 * Динамически настраивает свойства Spring Boot для подключения к запущенным контейнерам.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    // --- Контейнер PostgreSQL ---
    @Container
    static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:latest")
                    .withDatabaseName("bank_operation")
                    .withUsername("postgres")
                    .withPassword("testpassword");

    // --- Контейнер Cassandra ---
    @Container
    static final CassandraContainer<?> cassandraContainer =
            new CassandraContainer<>(DockerImageName.parse("cassandra:4.1"))
                    .withInitScript("init-cassandra-test.cql"); // Скрипт для создания keyspace в тестах!

    /**
     * Этот метод будет вызван ПОСЛЕ запуска контейнеров, но ДО создания контекста Spring.
     * Он позволяет динамически установить свойства Spring Boot, используя данные
     * из запущенных контейнеров (например, реальный порт, JDBC URL).
     *
     * @param registry Реестр свойств Spring.
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Переопределяем свойства для PostgreSQL
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl); // Получаем динамический JDBC URL
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Переопределяем свойства для Cassandra
        registry.add("spring.cassandra.contact-points", () -> cassandraContainer.getHost()); // Получаем хост контейнера
        registry.add("spring.cassandra.port", () -> cassandraContainer.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1"); // Должен совпадать с Cassandra
        registry.add("spring.cassandra.keyspace-name", () -> "bank_operation_test"); // Используем отдельный keyspace для тестов!
        registry.add("spring.cassandra.schema-action", () -> "create_if_not_exists"); // Позволяем Spring создавать таблицы

        registry.add("twelvedata.api.key", () -> "test-api-key"); // Предоставляем фиктивное значение
    }

    @BeforeAll
    static void setup() {
        System.out.println("PostgreSQL running: " + postgresContainer.isRunning());
        System.out.println("Cassandra running: " + cassandraContainer.isRunning());
        System.out.println("PostgreSQL JDBC URL: " + postgresContainer.getJdbcUrl());
        System.out.println("Cassandra Host: " + cassandraContainer.getHost());
        System.out.println("Cassandra Port: " + cassandraContainer.getMappedPort(9042));
    }

}
