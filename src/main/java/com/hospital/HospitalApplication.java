package com.hospital;

import com.hospital.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HospitalApplication {

    public static void main(String[] args) {
        // Automatically load environment variables from project-local .env if present
        DotenvLoader.load();

        SpringApplication.run(HospitalApplication.class, args);
    }
}
