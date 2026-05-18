package com.me;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.me.mapper")
public class TakeMeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TakeMeApplication.class, args);
    }
}