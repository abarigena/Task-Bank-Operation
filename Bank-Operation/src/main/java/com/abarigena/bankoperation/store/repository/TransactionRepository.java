package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Поиск транзакций, где превышен лимит расходов
    List<Transaction> findByLimitExceededTrue();

    // Методы поиска транзакций по категории и периоду времени
    List<Transaction> findByExpenseCategoryAndDateTimeBetween(
            Transaction.ExpenseCategory expenseCategory,
            LocalDateTime start,
            LocalDateTime end
    );

    // Расчет общих расходов по категориям в USD
    @Query("select t.expenseCategory, SUM(t.sumInUsd) from Transaction t group by t.expenseCategory")
    List<Object[]> calculateTotalSpendingInUsdByCategory();
}
