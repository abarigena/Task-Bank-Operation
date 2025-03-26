package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class TransactionDTO {

    @NotBlank(message = "Банковский счет клиента не может быть пустым")
    @JsonProperty("account_from")
    private String accountFrom;

    @NotBlank(message = "Банковский счет контрагента не может быть пустым")
    @JsonProperty("account_to")
    private String accountTo;

    @NotBlank(message = "Валюта счета должна быть указана")
    @Size(min = 3, max = 3, message = "Код валюты должен состоять из 3 символов")
    @JsonProperty("currency_shortname")
    private String currencyShortname;

    @NotNull(message = "Сумма транзакций должна быть указана")
    @Positive(message = "Сумма транзакций должна быть положительной")
    @JsonProperty("sum")
    private BigDecimal sum;

    @NotNull(message = "Категория расхода должна быть указана")
    @JsonProperty("expense_category")
    private Transaction.ExpenseCategory expenseCategory; // Enum из сущности

    @JsonProperty("datetime")
    private ZonedDateTime dateTime;
}
