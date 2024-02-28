package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FundTransferRequest {

  @NotNull
  @NotEmpty
  private final String fromAccountId;

  @NotNull
  @NotEmpty
  private final String toAccountId;

  @NotNull
  @Min(value = 0, message = "Transfer amount must be positive.")
  private BigDecimal amount;

//  public MoneyTranferRequest(String fromAccountId, String toAccountId,
//                             BigDecimal amount) {
//    this.fromAccountId = fromAccountId;
//    this.toAccountId = toAccountId;
//    this.amount = amount;
//  }

  @JsonCreator
  public FundTransferRequest(@JsonProperty("fromAccountId") String fromAccountId,
                             @JsonProperty("toAccountId") String toAccountId,
                             @JsonProperty("amount") BigDecimal amount) {
    this.fromAccountId = fromAccountId;
    this.toAccountId = toAccountId;
    this.amount = amount;
  }
}
