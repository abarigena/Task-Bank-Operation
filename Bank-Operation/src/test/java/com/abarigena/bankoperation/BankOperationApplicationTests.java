package com.abarigena.bankoperation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BankOperationApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        System.out.println("Spring context loaded successfully with Testcontainers!");
    }

}
