package com.abarigena.bankoperation.mapper;

import com.abarigena.bankoperation.dto.LimitDTO;
import com.abarigena.bankoperation.store.entity.ExpenseLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Маппер для преобразования между ExpenseLimit и LimitDTO.
 */
@Mapper(componentModel = "spring")
public interface LimitMapper {

    /**
     * Преобразует LimitDTO в сущность ExpenseLimit.
     * Устанавливает текущее время и валюту USD по умолчанию.
     *
     * @param dto DTO лимита.
     * @return Сущность ExpenseLimit.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "limitDateTime", expression = "java(java.time.ZonedDateTime.now())") // Устанавливаем текущее время
    @Mapping(target = "limitCurrencyShortname", constant = "USD") // Устанавливаем валюту
    ExpenseLimit toEntity(LimitDTO dto);
}
