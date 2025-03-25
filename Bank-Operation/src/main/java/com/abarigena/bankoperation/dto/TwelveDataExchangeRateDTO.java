package com.abarigena.bankoperation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class TwelveDataExchangeRateDTO {

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("values")
    private List<Value> values;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public List<Value> getValues() {
        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }

    // Вложенный класс для "meta"
    public static class Meta {
        private String symbol;
        private String interval;
        private String currencyBase;
        private String currencyQuote;
        private String type;

        // getters and setters
    }

    // Вложенный класс для "values"
    public static class Value {
        private String datetime;
        private String open;
        private String high;
        private String low;
        private String close;

        // getters and setters

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            this.open = open;
        }

        public String getHigh() {
            return high;
        }

        public void setHigh(String high) {
            this.high = high;
        }

        public String getLow() {
            return low;
        }

        public void setLow(String low) {
            this.low = low;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }

        public String getDatetime() {
            return datetime;
        }

        public void setDatetime(String datetime) {
            this.datetime = datetime;
        }
    }
}