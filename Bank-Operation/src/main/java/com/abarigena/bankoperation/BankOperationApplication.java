package com.abarigena.bankoperation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankOperationApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankOperationApplication.class, args);
    }

}
