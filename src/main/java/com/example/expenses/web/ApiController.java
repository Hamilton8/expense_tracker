package com.example.expenses.web;

import com.example.expenses.dto.Dtos.*;
import com.example.expenses.service.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ExpenseService expenses;
    private final CategoryService categories;

    public ApiController(ExpenseService expenses, CategoryService categories) {
        this.expenses = expenses; this.categories = categories;
    }

    @GetMapping("/categories") public List<CategoryResponse> categories() { return categories.list(); }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse addCategory(@RequestBody CategoryRequest r) { return categories.create(r.name()); }

    @GetMapping("/expenses")
    public List<ExpenseResponse> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) Long categoryId) {
        return expenses.list(from, to, categoryId);
    }

    @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse add(@RequestBody ExpenseRequest r) { return expenses.create(r); }

    @PutMapping("/expenses/{id}")
    public ExpenseResponse update(@PathVariable Long id, @RequestBody ExpenseRequest r) { return expenses.update(id, r); }

    @DeleteMapping("/expenses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { expenses.delete(id); }

    @GetMapping("/reports/summary")
    public SummaryResponse summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return expenses.summary(from, to);
    }

    @GetMapping("/reports/monthly")
    public MonthlyResponse monthly(@RequestParam int year) { return expenses.monthly(year); }
}
