package com.abarigena.bankoperation.store.entity;

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
@AllArgsConstructor
@Builder
public class ExchangeRate {
    @PrimaryKeyColumn(name = "from_currency", type = PrimaryKeyType.PARTITIONED)
    private String fromCurrency;

    @PrimaryKeyColumn(name = "to_currency", type = PrimaryKeyType.PARTITIONED)
    private String toCurrency;

    @PrimaryKeyColumn(name = "date", type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private LocalDate date;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private UUID id;

    @Column("close_price")
    private BigDecimal closePrice;

    @Column("previous_close_price")
    private BigDecimal previousClosePrice;
}
