package com.serveradmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.serveradmin.mapper")
@EnableScheduling
public class ServerAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerAdminApplication.class, args);
    }
}
