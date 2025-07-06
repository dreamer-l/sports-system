package com.example.sportsystem.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SportsRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsRegistryApplication.class, args);
    }
}