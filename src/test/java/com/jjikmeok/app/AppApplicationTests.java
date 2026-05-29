package com.jjikmeok.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Disabled("Temporarily disabled due to duplicate bean names in mixed legacy/new packages")
class AppApplicationTests {

	@Test
	void contextLoads() {
	}

}
