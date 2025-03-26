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

    @JsonProperty("values")
    private List<Value> values;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        @JsonProperty("datetime")
        private String datetime;

        @JsonProperty("close")
        private String close;
    }
}