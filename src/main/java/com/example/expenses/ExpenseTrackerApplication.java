package com.example.expenses;

import com.example.expenses.model.Category;
import com.example.expenses.repo.CategoryRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExpenseTrackerApplication {
    public static void main(String[] args) { SpringApplication.run(ExpenseTrackerApplication.class, args); }

    @Bean Clock clock() { return Clock.systemDefaultZone(); }

    @Bean CommandLineRunner seedCategories(CategoryRepository repo) {
        return args -> {
            if (repo.count() == 0)
                repo.saveAll(List.of("Food", "Transport", "Housing", "Utilities", "Entertainment", "Health", "Other")
                        .stream().map(Category::new).toList());
        };
    }
}
