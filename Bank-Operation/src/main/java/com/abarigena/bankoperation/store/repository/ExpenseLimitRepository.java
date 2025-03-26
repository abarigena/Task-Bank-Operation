package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, UUID> {

    // Поиск последнего установленного лимита для категории
    @Query("select el from ExpenseLimit el where el.expenseCategory = :category order by el.limitDateTime desc limit 1")
    Optional<ExpenseLimit> findLatestLimitByCategory(Transaction.ExpenseCategory category);

    // Поиск действующего лимита на определенную дату
    @Query("select el from ExpenseLimit el where el.expenseCategory = :category and el.limitDateTime <= :datetime " +
            "order by el.limitDateTime desc limit 1")
    Optional<ExpenseLimit> findLimitValidAtDateTime(
            @Param("category") Transaction.ExpenseCategory category,
            @Param("datetime") ZonedDateTime datetime
    );
}
