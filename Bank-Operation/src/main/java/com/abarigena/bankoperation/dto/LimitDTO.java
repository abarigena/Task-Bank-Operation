package com.abarigena.bankoperation.dto;

import com.abarigena.bankoperation.store.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Данные для установки нового месячного лимита расходов")
public class LimitDTO {

    @Schema(description = "Сумма нового лимита в USD", requiredMode = Schema.RequiredMode.REQUIRED, example = "1500.00")
    @NotNull(message = "Сумма лимита не должна быть пустой")
    @Positive(message = "Сумма лимита должна быть положительной")
    private BigDecimal limitSum; // Сумма нового лимита (в USD)

    @Schema(description = "Категория расходов, для которой устанавливается лимит", requiredMode = Schema.RequiredMode.REQUIRED, example = "SERVICE")
    @NotNull(message = "Категория расхода должна быть указана")
    private Transaction.ExpenseCategory expenseCategory;
}
