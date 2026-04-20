package com.campuslostfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
@SpringBootApplication
@EnableKnife4j
@EnableScheduling
public class CampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusApplication.class, args);
    }
}