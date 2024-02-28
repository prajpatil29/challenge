package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountException;
import com.dws.challenge.exception.InvalidAmountException;
import com.dws.challenge.exception.TransactionTimeoutException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,
                         NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void makeTransfer(String fromAccountId, String toAccountId,
                           BigDecimal amount) {
    if (fromAccountId == null || fromAccountId.isBlank()
            || toAccountId == null || toAccountId.isBlank()) {
      throw new InvalidAccountException("From account id and or to account id" +
              " cannot be empty.");
    }

    if (amount == null || amount.compareTo(BigDecimal.ZERO) == -1
            || amount.compareTo(BigDecimal.ZERO) == 0) {
      throw new InvalidAmountException("Amount cannot be less than or equal " +
              "to zero.");
    }

    Account fromAccount = this.accountsRepository.getAccount(fromAccountId);
    Account toAccount = this.accountsRepository.getAccount(toAccountId);

    boolean isLockAcquiredOnFromAccount = false;
    try {
      // Try to acquire lock on from account
      isLockAcquiredOnFromAccount = fromAccount.getLock().tryLock(10,
              TimeUnit.SECONDS);

      if (isLockAcquiredOnFromAccount) {
        boolean isLockAcquiredOnToAccount = false;
        try {
          // Try to acquire lock on to account
          isLockAcquiredOnToAccount = toAccount.getLock().tryLock(10,
                  TimeUnit.SECONDS);

          if (isLockAcquiredOnToAccount) {
            // If both locks are acquired, fetch the updated from and to
            // accounts from DB so that we can work on updated balance.
            fromAccount = this.accountsRepository.getAccount(fromAccountId);
            toAccount = this.accountsRepository.getAccount(toAccountId);

            if (fromAccount.getBalance().compareTo(amount) == -1) {
              throw new InsufficientFundsException("Insufficient funds. Check the " +
                      "fund balance before making fund transfer.");
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            notificationService.notifyAboutTransfer(fromAccount,
                    "Funds " + amount + " has been debited from your account.");
            notificationService.notifyAboutTransfer(toAccount,
                    "Funds " + amount + " has been credited to your account.");
          } else {
            throw new TransactionTimeoutException("Your transaction has timed" +
                    " out. Money will not be debited from your account. Please try " +
                    "again in some time.");
          }
        } finally {
          if (isLockAcquiredOnToAccount) {
            toAccount.getLock().unlock();
          }
        }
      } else {
        throw new TransactionTimeoutException("Your transaction has timed out" +
                ". Money will not be debited from your account. Please try " +
                "again in some time.");
      }
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
      if (isLockAcquiredOnFromAccount) {
        fromAccount.getLock().unlock();
      }
    }
  }
}
