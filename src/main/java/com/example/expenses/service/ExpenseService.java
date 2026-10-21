package com.example.expenses.service;

import static com.example.expenses.service.Errors.*;

import com.example.expenses.dto.Dtos.*;
import com.example.expenses.model.*;
import com.example.expenses.repo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseService {
    static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");
    static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1), MAX_DATE = LocalDate.of(9999, 12, 31);

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories, Clock clock) {
        this.expenses = expenses; this.categories = categories; this.clock = clock;
    }

    public ExpenseResponse create(ExpenseRequest r) { return toResponse(expenses.save(apply(new Expense(), r))); }

    public ExpenseResponse update(Long id, ExpenseRequest r) {
        Expense e = expenses.findById(id).orElseThrow(() -> notFound("Expense not found"));
        return toResponse(expenses.save(apply(e, r)));
    }

    public void delete(Long id) {
        if (!expenses.existsById(id)) throw notFound("Expense not found");
        expenses.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(LocalDate from, LocalDate to, Long categoryId) {
        LocalDate f = from == null ? MIN_DATE : from, t = to == null ? MAX_DATE : to;
        checkRange(f, t);
        List<Expense> found = categoryId == null ? expenses.findInRange(f, t) : expenses.findInRangeForCategory(categoryId, f, t);
        return found.stream().map(this::toResponse).toList();
    }

    /** Totals per category for a date range (defaults to the current month). */
    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate f = from == null ? today.withDayOfMonth(1) : from, t = to == null ? today : to;
        checkRange(f, t);
        List<Expense> found = expenses.findInRange(f, t);
        BigDecimal total = found.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> sums = found.stream().collect(Collectors.groupingBy(
                e -> e.getCategory().getName(), Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        List<CategoryTotal> rows = sums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(en -> new CategoryTotal(en.getKey(), en.getValue(), percent(en.getValue(), total))).toList();
        return new SummaryResponse(f, t, total, rows);
    }

    /** Spending per month for a year; months without expenses are 0. */
    @Transactional(readOnly = true)
    public MonthlyResponse monthly(int year) {
        if (year < 1970 || year > 9999) throw bad("Invalid year");
        Map<Integer, BigDecimal> months = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) months.put(m, BigDecimal.ZERO.setScale(2));
        for (Expense e : expenses.findInRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)))
            months.merge(e.getExpenseDate().getMonthValue(), e.getAmount(), BigDecimal::add);
        return new MonthlyResponse(year, months.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), months);
    }

    private Expense apply(Expense e, ExpenseRequest r) {
        BigDecimal a = r.amount();
        if (a == null || a.signum() <= 0) throw bad("Amount must be greater than zero");
        if (a.stripTrailingZeros().scale() > 2) throw bad("Amount allows at most 2 decimal places");
        if (a.compareTo(MAX_AMOUNT) > 0) throw bad("Amount too large");
        String desc = r.description() == null ? "" : r.description().trim();
        if (desc.length() > 255) throw bad("Description too long (max 255)");
        if (r.categoryId() == null) throw bad("categoryId is required");
        Category c = categories.findById(r.categoryId()).orElseThrow(() -> notFound("Category not found"));
        e.setAmount(a.setScale(2, RoundingMode.HALF_UP));
        e.setDescription(desc);
        e.setExpenseDate(r.date() == null ? LocalDate.now(clock) : r.date());
        e.setCategory(c);
        return e;
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw bad("'from' must not be after 'to'");
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(e.getId(), e.getAmount(), e.getDescription(), e.getExpenseDate(),
                e.getCategory().getId(), e.getCategory().getName());
    }
}
