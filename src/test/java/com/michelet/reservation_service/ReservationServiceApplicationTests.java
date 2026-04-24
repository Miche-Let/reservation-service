package com.michelet.reservation_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false"
        })
class ReservationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
