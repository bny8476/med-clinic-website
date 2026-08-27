package com.healthcare.clinic.finance.repository;

import com.healthcare.clinic.finance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByIncurredOnBetween(LocalDate startDate, LocalDate endDate);
    List<Expense> findAllByOrderByIncurredOnDesc();
    long countByStatus(com.healthcare.clinic.finance.entity.ExpenseStatus status);
}
