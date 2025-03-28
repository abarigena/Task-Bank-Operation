package com.abarigena.bankoperation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для представления курса обмена валют.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Сущность курса обмена валют")
public class ExchangeRateDTO {

    @Schema(description = "Код валюты, из которой конвертируют ", example = "EUR")
    private String fromCurrency;

    @Schema(description = "Код валюты, в которую конвертируют ", example = "USD")
    private String toCurrency;

    @Schema(description = "Дата, на которую действителен курс", example = "2025-03-28")
    private LocalDate date;

    @Schema(description = "Курс закрытия на указанную дату", example = "1.07719")
    private BigDecimal closePrice;

    @Schema(description = "Курс закрытия на предыдущий день", example = "1.077373")
    private BigDecimal previousClosePrice;
}
