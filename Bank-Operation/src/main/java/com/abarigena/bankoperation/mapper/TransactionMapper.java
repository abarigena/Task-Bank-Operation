package com.abarigena.bankoperation.mapper;

import com.abarigena.bankoperation.dto.TransactionDTO;
import com.abarigena.bankoperation.store.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.ZonedDateTime;

/**
 * Маппер для преобразования между Transaction и TransactionDTO.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {
    /**
     * Преобразует TransactionDTO в сущность Transaction.
     * Устанавливает текущее время, если dateTime в DTO равен null.
     *
     * @param dto DTO транзакции.
     * @return Сущность Transaction.
     */
    @Mapping(target = "id", ignore = true) // ID генерируется базой данных
    @Mapping(target = "sumInUsd", ignore = true) // Рассчитывается в сервисе
    @Mapping(target = "limitExceeded", ignore = true) // Рассчитывается в сервисе
    @Mapping(target = "dateTime", source = "dateTime", qualifiedByName = "mapDateTime") // Используем кастомный маппинг времени
    Transaction toEntity(TransactionDTO dto);

    // Кастомный метод для обработки ZonedDateTime
    // Если в DTO время не пришло (null), то ставим текущее
    @Named("mapDateTime")
    default ZonedDateTime mapDateTime(ZonedDateTime dtoDateTime) {
        return (dtoDateTime != null) ? dtoDateTime : ZonedDateTime.now();
    }
}
