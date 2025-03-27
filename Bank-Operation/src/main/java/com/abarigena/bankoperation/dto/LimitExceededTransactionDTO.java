package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO для представления транзакции, превысившей лимит,
 * вместе с деталями лимита, который был превышен.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimitExceededTransactionDTO {

    // Поля транзакции
    private UUID transactionId;
    private String accountFrom;
    private String accountTo;
    private String currencyShortname;
    private BigDecimal sum;
    private BigDecimal sumInUsd;
    private Transaction.ExpenseCategory expenseCategory;
    private ZonedDateTime transactionDateTime;

    // Поля соответствующего лимита
    private BigDecimal limitSum;
    private ZonedDateTime limitSetDateTime;
    private String limitCurrencyShortname;
}
