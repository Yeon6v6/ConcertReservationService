package kr.hhplus.be.server.api;

import jakarta.annotation.PreDestroy;
import org.testcontainers.containers.MySQLContainer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestcontainersConfig {

    public static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        // MySQL Testcontainer 초기화
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
//              .withInitScript("schema.sql")

        // 컨테이너 시작
        MYSQL_CONTAINER.start();

        // Spring Datasource 설정
        System.setProperty("spring.datasource.url", MYSQL_CONTAINER.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
        System.setProperty("spring.datasource.username", MYSQL_CONTAINER.getUsername());
        System.setProperty("spring.datasource.password", MYSQL_CONTAINER.getPassword());
    }

    // @PreDestroy를 이용한 종료 처리
    @PreDestroy
    public void preDestroy() {
        if (MYSQL_CONTAINER.isRunning()) {
            MYSQL_CONTAINER.stop();
        }
    }
}

