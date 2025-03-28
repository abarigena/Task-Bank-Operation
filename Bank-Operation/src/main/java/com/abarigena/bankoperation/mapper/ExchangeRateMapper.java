package com.abarigena.bankoperation.mapper;

import com.abarigena.bankoperation.dto.ExchangeRateDTO;
import com.abarigena.bankoperation.store.entity.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Маппер для преобразования между ExchangeRate и ExchangeRateDTO.
 */
@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    /**
     * Преобразует сущность ExchangeRate в ExchangeRateDTO.
     * @param entity Сущность курса обмена.
     * @return DTO курса обмена.
     */
    ExchangeRateDTO toDto(ExchangeRate entity);

    /**
     * Преобразует список сущностей ExchangeRate в список ExchangeRateDTO.
     * @param entities Список сущностей.
     * @return Список DTO.
     */
    List<ExchangeRateDTO> toDtoList(List<ExchangeRate> entities);
}
