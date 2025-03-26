package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Поиск транзакций, где превышен лимит расходов
    List<Transaction> findByLimitExceededTrue();

    // Расчет суммы расходов в USD за период для категории
    @Query("SELECT COALESCE(SUM(t.sumInUsd), 0) " +
            "FROM Transaction t " +
            "WHERE t.expenseCategory = :category " +
            "AND t.dateTime >= :periodStart AND t.dateTime < :periodEnd")
    BigDecimal calculateSpendingInUsdForPeriod(
            @Param("category") Transaction.ExpenseCategory expenseCategory,
            @Param("periodStart") ZonedDateTime periodStart,
            @Param("periodEnd") ZonedDateTime periodEnd
    );

    /*// Методы поиска транзакций по категории и периоду времени
    List<Transaction> findByExpenseCategoryAndDateTimeBetween(
            Transaction.ExpenseCategory expenseCategory,
            LocalDateTime start,
            LocalDateTime end
    );

    // Расчет общих расходов по категориям в USD
    @Query("select t.expenseCategory, SUM(t.sumInUsd) from Transaction t group by t.expenseCategory")
    List<Object[]> calculateTotalSpendingInUsdByCategory();*/
}
