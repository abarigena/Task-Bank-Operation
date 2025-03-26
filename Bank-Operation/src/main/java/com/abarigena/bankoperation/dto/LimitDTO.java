package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LimitDTO {

    @NotNull(message = "Сумма лимита не должна быть пустой")
    @Positive(message = "Сумма лимита должна быть положительной")
    private BigDecimal limitSum; // Сумма нового лимита (в USD)

    @NotNull(message = "Категория расхода должна быть указана")
    private Transaction.ExpenseCategory expenseCategory;
}
