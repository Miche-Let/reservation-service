package com.michelet.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false"
        }) // eureka client를 끄는 이유는, 테스트 시 eureka server가 켜져 있지 않아서 발생하는 문제를 방지하기 위함입니다. 실제로 eureka server가 켜져 있다면, 이 설정을 제거해도 됩니다.
class ReservationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
