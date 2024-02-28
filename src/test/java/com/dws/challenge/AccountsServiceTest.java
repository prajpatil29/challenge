package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.*;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId + "1");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount(uniqueAccountId + "1")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void makeTransfer_failsIfFromAccountIdIsInvalid() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    try {
      this.accountsService.makeTransfer(null, uniqueAccountId + "2", new BigDecimal("20"));
      fail("Should have failed while performing transfer if from account is " +
              "null");
    } catch (InvalidAccountException iae) {
      assertThat(iae.getMessage()).isEqualTo("From account id and or to " +
              "account id cannot be empty.");
    }

    try {
      this.accountsService.makeTransfer("", uniqueAccountId + "2", new BigDecimal("20"));
      fail("Should have failed while performing transfer if from account is " +
              "empty");
    } catch (InvalidAccountException iae) {
      assertThat(iae.getMessage()).isEqualTo("From account id and or to " +
              "account id cannot be empty.");
    }
  }

  @Test
  void makeTransfer_failsIfToAccountIdIsInvalid() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", null, new BigDecimal("20"));
      fail("Should have failed while performing transfer if to account is " +
              "null");
    } catch (InvalidAccountException iae) {
      assertThat(iae.getMessage()).isEqualTo("From account id and or to " +
              "account id cannot be empty.");
    }

    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", "", new BigDecimal("20"));
      fail("Should have failed while performing transfer if to account is " +
              "empty");
    } catch (InvalidAccountException iae) {
      assertThat(iae.getMessage()).isEqualTo("From account id and or to " +
              "account id cannot be empty.");
    }
  }

  @Test
  void makeTransfer_failsIfTransferAmountIsInvalid() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2", null);
      fail("Should have failed while performing transfer if transfer amount " +
              "is null");
    } catch (InvalidAmountException iae) {
      assertThat(iae.getMessage()).isEqualTo("Amount cannot be less than " +
              "or equal to zero.");
    }

    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2", new BigDecimal(
              "-1"));
      fail("Should have failed while performing transfer if transfer amount " +
              "is negative");
    } catch (InvalidAmountException iae) {
      assertThat(iae.getMessage()).isEqualTo("Amount cannot be less than " +
              "or equal to zero.");
    }

    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2", new BigDecimal("0"));
      fail("Should have failed while performing transfer if transfer amount " +
              "is zero");
    } catch (InvalidAmountException iae) {
      assertThat(iae.getMessage()).isEqualTo("Amount cannot be less than " +
              "or equal to zero.");
    }
  }

  @Test
  void makeTransfer_failsIfInsufficientFunds() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account fromAccount = new Account(uniqueAccountId + "1");
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(toAccount);

    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2", new BigDecimal("20"));
      fail("Should have failed while performing transfer if insufficient " +
              "funds");
    } catch (InsufficientFundsException ife) {
      assertThat(ife.getMessage()).isEqualTo("Insufficient funds. Check the " +
              "fund balance before making fund transfer.");
    }
  }

  @Test
  void makeTransfer() {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account fromAccount = new Account(uniqueAccountId + "1");
    fromAccount.setBalance(new BigDecimal("200.90"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(toAccount);

    try {
      this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2",
              new BigDecimal("20.70"));
      assertThat(this.accountsService.getAccount(uniqueAccountId + "1").getBalance()).isEqualTo("180.20");
      assertThat(this.accountsService.getAccount(uniqueAccountId + "2").getBalance()).isEqualTo("20.70");
    } catch (Exception e) {
      fail("Fund transfer should be successful");
    }
  }

  @Test
  void makeTransfer_GivesTimeoutExceptionIfOtherTransactionIsGoingOnFromAccount() throws InterruptedException {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account fromAccount = new Account(uniqueAccountId + "1");
    fromAccount.setBalance(new BigDecimal("200.90"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(toAccount);

    AtomicBoolean isFailed = new AtomicBoolean(false);

    // Hold lock on from account with current running thread
    fromAccount.getLock().lock();

    Thread t1 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "1",
                uniqueAccountId + "2", new BigDecimal("20.70"));
        isFailed.set(true);
      } catch (TransactionTimeoutException tte) {
        assertThat(tte.getMessage()).isEqualTo("Your transaction has timed out. " +
                "Money will not be debited from your account. Please try again in some time.");
      }
    });

    t1.start();
    t1.join();

    if (isFailed.get()) {
      fail("Should have failed while performing transfer if other " +
              "transaction is going on from account");
    }
  }

  @Test
  void makeTransfer_GivesTimeoutExceptionIfOtherTransactionIsGoingOnToAccount() throws InterruptedException {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account fromAccount = new Account(uniqueAccountId + "1");
    fromAccount.setBalance(new BigDecimal("200.90"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(toAccount);

    AtomicBoolean isFailed = new AtomicBoolean(false);

    // Hold lock on to account with current running thread
    toAccount.getLock().lock();

    Thread t1 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "1",
                uniqueAccountId + "2", new BigDecimal("20.70"));
        isFailed.set(true);
      } catch (TransactionTimeoutException tte) {
        assertThat(tte.getMessage()).isEqualTo("Your transaction has timed out. " +
                "Money will not be debited from your account. Please try again in some time.");
      }
    });

    t1.start();
    t1.join();

    if (isFailed.get()) {
      fail("Should have failed while performing transfer if other " +
              "transaction is going on to account");
    }
  }

  @Test
  void makeTransfer_WithParallelRequestsOnThreeAccounts() throws InterruptedException {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account account1 = new Account(uniqueAccountId + "1");
    account1.setBalance(new BigDecimal("200.90"));
    this.accountsService.createAccount(account1);
    Account account2 = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(account2);
    Account account3 = new Account(uniqueAccountId + "3");
    account3.setBalance(new BigDecimal("20"));
    this.accountsService.createAccount(account3);

    AtomicBoolean isFailed = new AtomicBoolean(false);
    Thread t1 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2",
                new BigDecimal("20.70"));
      } catch (Exception e) {
        isFailed.set(true);
      }
    });

    Thread t2 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "3",
                new BigDecimal("30"));
      } catch (Exception e) {
        isFailed.set(true);
      }
    });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertThat(this.accountsService.getAccount(uniqueAccountId + "1").getBalance()).isEqualTo("150.20");
    assertThat(this.accountsService.getAccount(uniqueAccountId + "2").getBalance()).isEqualTo("20.70");
    assertThat(this.accountsService.getAccount(uniqueAccountId + "3").getBalance()).isEqualTo("50");

    if (isFailed.get()) {
      fail("Fund transfer should be successful");
    }
  }

  @Test
  void makeTransfer_WithParallelRequestsOnFourAccounts() throws InterruptedException {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();

    Account account1 = new Account(uniqueAccountId + "1");
    account1.setBalance(new BigDecimal("200.90"));
    this.accountsService.createAccount(account1);
    Account account2 = new Account(uniqueAccountId + "2");
    this.accountsService.createAccount(account2);

    Account account3 = new Account(uniqueAccountId + "3");
    account3.setBalance(new BigDecimal("20"));
    this.accountsService.createAccount(account3);
    Account account4 = new Account(uniqueAccountId + "4");
    account4.setBalance(new BigDecimal("230"));
    this.accountsService.createAccount(account4);

    AtomicBoolean isFailed = new AtomicBoolean(false);
    Thread t1 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "1", uniqueAccountId + "2",
                new BigDecimal("20.70"));
      } catch (Exception e) {
        isFailed.set(true);
      }
    });

    Thread t2 = new Thread(() -> {
      try {
        this.accountsService.makeTransfer(uniqueAccountId + "4",
                uniqueAccountId + "3", new BigDecimal("30"));
      } catch (Exception e) {
        isFailed.set(true);
      }
    });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertThat(this.accountsService.getAccount(uniqueAccountId + "1").getBalance()).isEqualTo("180.20");
    assertThat(this.accountsService.getAccount(uniqueAccountId + "2").getBalance()).isEqualTo("20.70");
    assertThat(this.accountsService.getAccount(uniqueAccountId + "3").getBalance()).isEqualTo("50");
    assertThat(this.accountsService.getAccount(uniqueAccountId + "4").getBalance()).isEqualTo("200");

    if (isFailed.get()) {
      fail("Fund transfer should be successful");
    }
  }
}
