package com.example.expenses.repo;

import com.example.expenses.model.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("select e from Expense e join fetch e.category where e.expenseDate between :from and :to order by e.expenseDate desc, e.id desc")
    List<Expense> findInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e from Expense e join fetch e.category where e.category.id = :cat and e.expenseDate between :from and :to order by e.expenseDate desc, e.id desc")
    List<Expense> findInRangeForCategory(@Param("cat") Long categoryId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
