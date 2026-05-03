package com.michelet.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class ReservationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
