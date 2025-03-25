package com.abarigena.bankoperation.store.entity;

import jakarta.persistence.Entity;
import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Table(value = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ExchangeRate {
    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.PARTITIONED)
    private UUID id;

    @PrimaryKeyColumn(name = "from_currency", type = PrimaryKeyType.PARTITIONED)
    private String fromCurrency;

    @PrimaryKeyColumn(name = "to_currency", type = PrimaryKeyType.PARTITIONED)
    private String toCurrency;

    @PrimaryKeyColumn(name = "date", type = PrimaryKeyType.CLUSTERED)
    private LocalDate date;

    @Column("close_price")
    private BigDecimal closePrice;

    @Column("previous_close_price")
    private BigDecimal previousClosePrice;

    public ExchangeRate(UUID id, String fromCurrency, String toCurrency, LocalDate date
            , BigDecimal closePrice, BigDecimal previousClosePrice) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.date = date;
        this.closePrice = closePrice;
        this.previousClosePrice = previousClosePrice;
    }
}
