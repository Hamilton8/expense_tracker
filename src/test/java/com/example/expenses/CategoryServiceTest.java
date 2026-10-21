package com.example.expenses;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.expenses.model.Category;
import com.example.expenses.repo.CategoryRepository;
import com.example.expenses.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repo;
    @InjectMocks CategoryService service;

    @Test void createsTrimmedCategory() {
        when(repo.existsByNameIgnoreCase("Pets")).thenReturn(false);
        when(repo.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Pets", service.create("  Pets ").name());
    }
    @Test void duplicateNameIs409() {
        when(repo.existsByNameIgnoreCase("Food")).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service.create("Food"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"   "})
    void blankNameIs400(String name) {
        var ex = assertThrows(ResponseStatusException.class, () -> service.create(name));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
    @Test void tooLongNameIs400() {
        var ex = assertThrows(ResponseStatusException.class, () -> service.create("x".repeat(61)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
