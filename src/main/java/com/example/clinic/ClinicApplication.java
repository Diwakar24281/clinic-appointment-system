package com.example.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" Clinic Appointment System Started Successfully! ");
        System.out.println(" Web Portal: http://localhost:8080/pages/index.html ");
        System.out.println("=================================================");
    }

    /**
     * Password encoder bean used across the application to securely hash and verify passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
