package com.abarigena.bankoperation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwelveDataExchangeRateDTO {

    // Оставляем только 'values', так как 'meta' не используется
    @JsonProperty("values")
    private List<Value> values;

    // Вложенный класс для элементов списка "values"
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        // Оставляем только 'datetime' и 'close'
        @JsonProperty("datetime")
        private String datetime; // Оставляем как String, т.к. не используется напрямую для даты в логике

        @JsonProperty("close")
        private String close; // Оставляем как String для последующей конвертации в BigDecimal
    }
}