package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isCreated());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void transferFundsNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferFundsInvalidFromAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("{\"fromAccountId\":\"\"," +
                    "\"toAccountId\":\"Id-123\"," +
                    "\"amount\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("From account id and or to account id cannot be empty."));
  }

  @Test
  void transferFundsInvalidToAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("{\"fromAccountId\":\"Id-123\"," +
                    "\"toAccountId\":\"\"," +
                    "\"amount\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("From account id and or to account id cannot be empty."));
  }

  @Test
  void transferFundsWithNegativeAmount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("{\"fromAccountId\":\"Id-123\"," +
                            "\"toAccountId\":\"Id-456\"," +
                            "\"amount\":-100}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Amount cannot be less than or equal to zero."));
  }

  @Test
  void transferFundsWithZeroAmount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("{\"fromAccountId\":\"Id-123\"," +
                            "\"toAccountId\":\"Id-456\"," +
                            "\"amount\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Amount cannot be less than or equal to zero."));
  }

  @Test
  void transferFundsInsufficientFunds() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account fromAccount = new Account(uniqueAccountId + "1", new BigDecimal(
            "90"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2", new BigDecimal(
            "10"));
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("{\"fromAccountId\":\"" + uniqueAccountId + "1\"," +
                    "\"toAccountId\":\"" + uniqueAccountId + "2\"," +
                    "\"amount\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Insufficient funds. Check the fund balance before making fund transfer."));
  }

  @Test
  void transferFundsTransactionTimeOut() throws InterruptedException {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account fromAccount = new Account(uniqueAccountId + "1", new BigDecimal(
            "90"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2", new BigDecimal(
            "10"));
    this.accountsService.createAccount(toAccount);

    // Hold lock on from account with current running thread
    fromAccount.getLock().lock();

    Thread t1 = new Thread(() -> {
      try {
          this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
                          .content("{\"fromAccountId\":\"" + uniqueAccountId + "1\"," +
                                  "\"toAccountId\":\"" + uniqueAccountId + "2\"," +
                                  "\"amount\":100}"))
                  .andExpect(status().isInternalServerError())
                  .andExpect(content().string("Your transaction has timed out. Money will not be debited from your account. Please try again in some time."));
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    });

    t1.start();
    t1.join();
  }

  @Test
  void transferFunds() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account fromAccount = new Account(uniqueAccountId + "1", new BigDecimal(
            "123.45"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account(uniqueAccountId + "2", new BigDecimal(
            "10"));
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transferFunds").contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("{\"fromAccountId\":\"" + uniqueAccountId + "1\"," +
                    "\"toAccountId\":\"" + uniqueAccountId + "2\"," +
                    "\"amount\":100}")).andExpect(status().isOk());

    assertThat(fromAccount.getAccountId()).isEqualTo(uniqueAccountId + "1");
    assertThat(fromAccount.getBalance()).isEqualByComparingTo("23.45");
    assertThat(toAccount.getAccountId()).isEqualTo(uniqueAccountId + "2");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("110");
  }
}
