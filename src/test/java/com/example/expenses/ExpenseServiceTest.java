package com.example.expenses;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.expenses.dto.Dtos.*;
import com.example.expenses.model.*;
import com.example.expenses.repo.*;
import com.example.expenses.service.ExpenseService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);
    static final LocalDate TODAY = LocalDate.of(2026, 9, 30);
    @Mock ExpenseRepository expenses;
    @Mock CategoryRepository categories;
    ExpenseService service;
    final Category food = new Category("Food"), travel = new Category("Transport");

    @BeforeEach void setUp() { service = new ExpenseService(expenses, categories, CLOCK); }

    static HttpStatus statusOf(Executable e) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, e).getStatusCode();
    }
    static BigDecimal bd(String s) { return new BigDecimal(s); }
    Expense exp(String amount, LocalDate date, Category c) {
        Expense e = new Expense(); e.setAmount(bd(amount)); e.setExpenseDate(date); e.setCategory(c); return e;
    }

    // ---- create / update / delete ----
    @Test void createsExpenseAndNormalisesAmount() {
        when(categories.findById(1L)).thenReturn(Optional.of(food));
        when(expenses.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));
        ExpenseResponse r = service.create(new ExpenseRequest(bd("10.5"), "  Lunch ", LocalDate.of(2026, 9, 1), 1L));
        assertEquals(bd("10.50"), r.amount());
        assertEquals("Lunch", r.description());
        assertEquals("Food", r.category());
        assertEquals(LocalDate.of(2026, 9, 1), r.date());
    }
    @Test void missingDateDefaultsToToday() {
        when(categories.findById(1L)).thenReturn(Optional.of(food));
        when(expenses.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(TODAY, service.create(new ExpenseRequest(bd("5"), null, null, 1L)).date());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-5", "1.234", "10000000000"})
    void rejectsInvalidAmounts(String amount) {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(new ExpenseRequest(bd(amount), "x", TODAY, 1L))));
        verify(expenses, never()).save(any());
    }
    @Test void rejectsNullAmount() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(new ExpenseRequest(null, "x", TODAY, 1L))));
    }
    @Test void rejectsMissingCategoryId() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(new ExpenseRequest(bd("5"), "x", TODAY, null))));
    }
    @Test void rejectsTooLongDescription() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(new ExpenseRequest(bd("5"), "a".repeat(256), TODAY, 1L))));
    }
    @Test void unknownCategoryIs404() {
        when(categories.findById(9L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.create(new ExpenseRequest(bd("5"), "x", TODAY, 9L))));
    }
    @Test void updatesExistingExpense() {
        Expense e = exp("1.00", TODAY, food);
        when(expenses.findById(5L)).thenReturn(Optional.of(e));
        when(categories.findById(2L)).thenReturn(Optional.of(travel));
        when(expenses.save(e)).thenReturn(e);
        ExpenseResponse r = service.update(5L, new ExpenseRequest(bd("20"), "Taxi", TODAY, 2L));
        assertEquals("Transport", r.category());
        assertEquals(bd("20.00"), r.amount());
    }
    @Test void updateMissingExpenseIs404() {
        when(expenses.findById(5L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.update(5L, new ExpenseRequest(bd("1"), "x", TODAY, 1L))));
    }
    @Test void deletesExistingExpense() {
        when(expenses.existsById(5L)).thenReturn(true);
        service.delete(5L);
        verify(expenses).deleteById(5L);
    }
    @Test void deleteMissingExpenseIs404() {
        when(expenses.existsById(5L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.delete(5L)));
        verify(expenses, never()).deleteById(any());
    }

    // ---- list ----
    @Test void listWithoutCategoryUsesRangeQuery() {
        LocalDate f = LocalDate.of(2026, 9, 1);
        when(expenses.findInRange(f, TODAY)).thenReturn(List.of(exp("3.00", TODAY, food)));
        assertEquals(1, service.list(f, TODAY, null).size());
    }
    @Test void listWithCategoryUsesCategoryQuery() {
        when(expenses.findInRangeForCategory(1L, ExpenseService.MIN_DATE, ExpenseService.MAX_DATE)).thenReturn(List.of());
        assertTrue(service.list(null, null, 1L).isEmpty());
    }
    @Test void listRejectsReversedRange() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.list(TODAY, TODAY.minusDays(1), null)));
    }

    // ---- reports ----
    @Test void summaryTotalsAndPercentagesPerCategory() {
        LocalDate f = LocalDate.of(2026, 9, 1);
        when(expenses.findInRange(f, TODAY)).thenReturn(List.of(
                exp("30.00", TODAY, food), exp("10.00", TODAY, food), exp("60.00", TODAY, travel)));
        SummaryResponse s = service.summary(f, TODAY);
        assertEquals(bd("100.00"), s.total());
        assertEquals(2, s.byCategory().size());
        assertEquals("Transport", s.byCategory().get(0).category());
        assertEquals(bd("60.0"), s.byCategory().get(0).percent());
        assertEquals(bd("40.00"), s.byCategory().get(1).total());
    }
    @Test void summaryDefaultsToCurrentMonth() {
        when(expenses.findInRange(LocalDate.of(2026, 9, 1), TODAY)).thenReturn(List.of());
        SummaryResponse s = service.summary(null, null);
        assertEquals(BigDecimal.ZERO, s.total());
        assertTrue(s.byCategory().isEmpty());
    }
    @Test void summaryRejectsReversedRange() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.summary(TODAY, TODAY.minusDays(1))));
    }
    @Test void monthlyFillsAllTwelveMonths() {
        when(expenses.findInRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(List.of(
                exp("10.00", LocalDate.of(2026, 1, 5), food), exp("5.50", LocalDate.of(2026, 1, 31), food), exp("7.00", LocalDate.of(2026, 12, 1), travel)));
        MonthlyResponse m = service.monthly(2026);
        assertEquals(12, m.byMonth().size());
        assertEquals(bd("15.50"), m.byMonth().get(1));
        assertEquals(0, m.byMonth().get(6).signum());
        assertEquals(bd("7.00"), m.byMonth().get(12));
        assertEquals(bd("22.50"), m.total());
    }
    @Test void monthlyRejectsInvalidYear() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.monthly(1800)));
    }
}
