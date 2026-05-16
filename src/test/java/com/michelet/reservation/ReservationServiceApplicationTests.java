package com.michelet.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class ReservationServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("create-schema.sql");

    @Test
    void contextLoads() {
    }

}
