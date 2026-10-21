package com.mss301.inventory.config;

import com.mss301.inventory.entity.Product;
import com.mss301.inventory.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new Product("P1", "Laptop", 1000, 10),
                        new Product("P2", "Mouse", 20, 5)));
            }
        };
    }
}
