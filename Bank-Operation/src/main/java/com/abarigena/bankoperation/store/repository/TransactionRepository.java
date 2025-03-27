package com.abarigena.bankoperation.store.repository;

import com.abarigena.bankoperation.dto.LimitExceededTransactionDTO;
import com.abarigena.bankoperation.store.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями Транзакций (Transaction).
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Рассчитывает общую сумму расходов (в USD) для указанной категории
     * за определенный период времени (включая начальную дату, исключая конечную).
     * Если транзакций за период нет, возвращает 0.
     *
     * @param expenseCategory Категория расходов (PRODUCT или SERVICE).
     * @param periodStart     Начало периода (включительно).
     * @param periodEnd       Конец периода (исключительно).
     * @return Сумма расходов в USD за период, или 0, если транзакций не было.
     */
    @Query("SELECT COALESCE(SUM(t.sumInUsd), 0) " +
            "FROM Transaction t " +
            "WHERE t.expenseCategory = :category " +
            "AND t.dateTime >= :periodStart AND t.dateTime < :periodEnd")
    BigDecimal calculateSpendingInUsdForPeriod(
            @Param("category") Transaction.ExpenseCategory expenseCategory,
            @Param("periodStart") ZonedDateTime periodStart,
            @Param("periodEnd") ZonedDateTime periodEnd
    );

    /**
     * Находит все транзакции, у которых флаг limitExceeded установлен в true.
     * Для каждой такой транзакции подбирает самый последний лимит (ExpenseLimit),
     * который был установлен для её категории расходов НЕ ПОЗЖЕ времени самой транзакции.
     * Результат сразу маппится в список объектов LimitExceededTransactionDTO,
     * содержащих как детали транзакции, так и детали превышенного лимита.
     *
     * @return Список DTO с транзакциями, превысившими лимит, и информацией о лимите.
     *         Список отсортирован по времени транзакции в порядке убывания (сначала самые новые).
     */
    @Query("SELECT new com.abarigena.bankoperation.dto.LimitExceededTransactionDTO(" +
            "t.id, t.accountFrom, t.accountTo, t.currencyShortname, t.sum, t.sumInUsd, t.expenseCategory, t.dateTime, " +
            "el.limitSum, el.limitDateTime, el.limitCurrencyShortname) " +
            "FROM Transaction t " +
            // Присоединяем лимиты (el), которые подходят по категории и установлены не позже транзакции (t.dateTime)
            "LEFT JOIN ExpenseLimit el ON t.expenseCategory = el.expenseCategory AND el.limitDateTime <= t.dateTime " +
            "WHERE t.limitExceeded = true " +
            "AND NOT EXISTS (" +
            "  SELECT el2 FROM ExpenseLimit el2 " +
            "  WHERE el2.expenseCategory = el.expenseCategory " +
            "  AND el2.limitDateTime <= t.dateTime " +
            "  AND el2.limitDateTime > el.limitDateTime" +
            ") " +
            "ORDER BY t.dateTime DESC")
    List<LimitExceededTransactionDTO> findExceededTransactionsWithLimitDetails();
}
