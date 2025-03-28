package com.abarigena.bankoperation.dto;

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
public class ExchangeRateDTO {

    private String fromCurrency;
    private String toCurrency;
    private LocalDate date;
    private BigDecimal closePrice;
    private BigDecimal previousClosePrice;
}
