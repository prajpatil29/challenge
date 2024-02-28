package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.FundTransferRequest;
import com.dws.challenge.exception.*;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
      this.accountsService.createAccount(account);
    } catch (InvalidAccountException | DuplicateAccountIdException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping(path = "/transferFunds", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferFunds(@RequestBody @Valid FundTransferRequest fundTransferRequest) {
    log.info("Making money transfer of amount {} from account id {} to " +
                    "account id {}", fundTransferRequest.getAmount(),
            fundTransferRequest.getFromAccountId(), fundTransferRequest.getToAccountId());
    try {
      this.accountsService.makeTransfer(fundTransferRequest.getFromAccountId(),
              fundTransferRequest.getToAccountId(), fundTransferRequest.getAmount());
    } catch (InvalidAccountException | InvalidAmountException | InsufficientFundsException ie) {
      return new ResponseEntity<>(ie.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (TransactionTimeoutException tte) {
      return new ResponseEntity<>(tte.getMessage(),
              HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      return new ResponseEntity<>("An error occurred while transferring " +
              "funds. Funds are not debited from your account.",
              HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }
}
