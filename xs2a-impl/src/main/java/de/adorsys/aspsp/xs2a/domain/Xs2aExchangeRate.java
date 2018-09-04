/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.aspsp.xs2a.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Currency;

@Data
@ApiModel(description = "Exchange Rate ", value = "ExchangeRate")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xs2aExchangeRate {

    @ApiModelProperty(value = "ISO 4217 currency code", required = true, example = "EUR")
    @NotNull
    private Currency currencyFrom;

    @ApiModelProperty(value = "rateFrom", required = true)
    @NotNull
    private String rateFrom;

    @ApiModelProperty(value = "ISO 4217 currency code", required = true, example = "EUR")
    @NotNull
    private Currency currencyTo;

    @ApiModelProperty(value = "rateTo", required = true)
    @NotNull
    private String rateTo;

    @ApiModelProperty(value = "Rate date", required = true, example = "2017-01-01")
    @NotNull
    private LocalDate rateDate;

    @ApiModelProperty(value = "Rate contract")
    @NotNull
    private String rateContract;
}