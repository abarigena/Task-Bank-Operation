package com.abarigena.bankoperation.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @NotBlank(message = "Банковский счет клиента не может быть пустым")
    private String accountFrom;

    @NotBlank(message = "Банковский счет контрагента не может быть пустым")
    private String accountTo;

    @NotBlank(message = "Валюта счета должна быть указана")
    private String currencyShortname;

    @NotNull(message = "Сумма транзакций должна быть указана")
    @Positive(message = "Сумма транзакций должна быть положительной")
    private BigDecimal sum;

    @NotNull(message = "USD sum is required")
    @Positive(message = "USD sum must be positive")
    @Column(name = "sum_in_usd")
    private BigDecimal sumInUsd;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory expenseCategory;

    @NotNull(message = "Время транзакции должно быть указано")
    @Column(name = "datetime")
    private ZonedDateTime dateTime;

    private Boolean limitExceeded;

    public enum ExpenseCategory {
        PRODUCT,SERVICE
    }
}
