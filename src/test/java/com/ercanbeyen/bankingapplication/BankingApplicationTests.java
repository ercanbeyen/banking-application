package com.ercanbeyen.bankingapplication;

import com.ercanbeyen.bankingapplication.config.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = BankingApplicationTests.class)
@Import(TestContainerConfig.class)
class BankingApplicationTests {

	@Test
	void contextLoads() {}
}
