package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Schema(description = "Данные о новой транзакции, полученные от клиента")
public class TransactionDTO {

    @Schema(description = "Банковский счет клиента (отправителя)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1000000001")
    @NotBlank(message = "Банковский счет клиента не может быть пустым")
    @JsonProperty("account_from")
    private String accountFrom;

    @Schema(description = "Банковский счет контрагента (получателя)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "9999999999")
    @NotBlank(message = "Банковский счет контрагента не может быть пустым")
    @JsonProperty("account_to")
    private String accountTo;

    @Schema(description = "Код валюты счета транзакции ",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "KZT", minLength = 3, maxLength = 3)
    @NotBlank(message = "Валюта счета должна быть указана")
    @Size(min = 3, max = 3, message = "Код валюты должен состоять из 3 символов")
    @JsonProperty("currency_shortname")
    private String currencyShortname;

    @Schema(description = "Сумма транзакции (должна быть положительной)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "150000.50")
    @NotNull(message = "Сумма транзакций должна быть указана")
    @Positive(message = "Сумма транзакций должна быть положительной")
    @JsonProperty("sum")
    private BigDecimal sum;

    @Schema(description = "Категория расходов ('PRODUCT' или 'SERVICE')",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "SERVICE")
    @NotNull(message = "Категория расхода должна быть указана")
    @JsonProperty("expense_category")
    private Transaction.ExpenseCategory expenseCategory; // Enum из сущности

    @Schema(description = "Дата и время совершения транзакции . Если не указано, используется текущее время сервера.",
            nullable = true, example = "2025-03-28T10:11:12.437Z")
    @JsonProperty("datetime")
    private ZonedDateTime dateTime;
}
