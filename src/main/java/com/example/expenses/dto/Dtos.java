package com.example.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}
    public record CategoryRequest(String name) {}
    public record CategoryResponse(Long id, String name) {}
    public record ExpenseRequest(BigDecimal amount, String description, LocalDate date, Long categoryId) {}
    public record ExpenseResponse(Long id, BigDecimal amount, String description, LocalDate date, Long categoryId, String category) {}
    public record CategoryTotal(String category, BigDecimal total, BigDecimal percent) {}
    public record SummaryResponse(LocalDate from, LocalDate to, BigDecimal total, List<CategoryTotal> byCategory) {}
    public record MonthlyResponse(int year, BigDecimal total, Map<Integer, BigDecimal> byMonth) {}
}
