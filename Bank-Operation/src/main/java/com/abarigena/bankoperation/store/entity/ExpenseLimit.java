package com.abarigena.bankoperation.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Entity
@Table(name = "expense_limits")
public class ExpenseLimit {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @NotNull(message = "Сумма лимита не должна быть пустой")
    @Positive(message = "Сумма лимита должна быть положительной")
    private BigDecimal limitSum;

    @NotNull(message = "Время не должно быть пустым")
    @Column(name = "limit_datetime")
    private ZonedDateTime limitDateTime;

    @NotNull(message = "Сохращение валюты не должно быть пустым")
    private String limitCurrencyShortname;

    @Enumerated(EnumType.STRING)
    private Transaction.ExpenseCategory expenseCategory;

}
