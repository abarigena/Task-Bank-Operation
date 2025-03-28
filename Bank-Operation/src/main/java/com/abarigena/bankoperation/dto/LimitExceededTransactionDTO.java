package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Транзакция, превысившая лимит, с деталями примененного лимита")
public class LimitExceededTransactionDTO {

    // Поля транзакции
    @Schema(description = "Уникальный идентификатор транзакции")
    private UUID transactionId;

    @Schema(description = "Счет клиента, с которого списаны средства", example = "1000000001")
    private String accountFrom;

    @Schema(description = "Счет контрагента, на который зачислены средства", example = "9999999999")
    private String accountTo;

    @Schema(description = "Код валюты транзакции ", example = "KZT")
    private String currencyShortname;

    @Schema(description = "Сумма транзакции в оригинальной валюте", example = "150000.50")
    private BigDecimal sum;

    @Schema(description = "Сумма транзакции, конвертированная в USD", example = "300.25")
    private BigDecimal sumInUsd;

    @Schema(description = "Категория расходов транзакции", example = "SERVICE")
    private Transaction.ExpenseCategory expenseCategory;

    @Schema(description = "Дата и время совершения транзакции")
    private ZonedDateTime transactionDateTime;

    // Поля соответствующего лимита
    @Schema(description = "Сумма лимита (в USD), который действовал на момент транзакции", example = "2000.00")
    private BigDecimal limitSum;

    @Schema(description = "Дата и время установки лимита, который действовал на момент транзакции")
    private ZonedDateTime limitSetDateTime;

    @Schema(description = "Валюта лимита (всегда USD)", example = "USD")
    private String limitCurrencyShortname;
}
