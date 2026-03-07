package com.fierceadventurer.smartportfoliobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartPortfolioBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPortfolioBackendApplication.class, args);
    }

}
