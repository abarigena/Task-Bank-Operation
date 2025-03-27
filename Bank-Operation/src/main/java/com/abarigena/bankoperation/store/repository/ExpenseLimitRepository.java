package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import com.abarigena.bankoperation.store.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями Лимитов Расходов (ExpenseLimit).
 */
public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, UUID> {

    /**
     * Находит лимит расходов, который был *действующим* для указанной категории
     * в определенный момент времени (дату и время).
     * Возвращает самый последний лимит, установленный НЕ ПОЗЖЕ указанного dateTime.
     *
     * @param category Категория расходов (PRODUCT или SERVICE).
     * @param datetime Дата и время, на которое нужно определить действующий лимит.
     * @return Optional, содержащий лимит, действовавший в указанный момент времени,
     *         или пустой Optional, если ни один лимит для этой категории не был установлен к этому времени.
     */
    @Query("select el from ExpenseLimit el where el.expenseCategory = :category and el.limitDateTime <= :datetime " +
            "order by el.limitDateTime desc limit 1")
    Optional<ExpenseLimit> findLimitValidAtDateTime(
            @Param("category") Transaction.ExpenseCategory category,
            @Param("datetime") ZonedDateTime datetime
    );
}
