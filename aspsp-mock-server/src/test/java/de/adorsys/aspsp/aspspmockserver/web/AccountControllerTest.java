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

package de.adorsys.aspsp.aspspmockserver.web;

import de.adorsys.aspsp.aspspmockserver.service.AccountService;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountBalance;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiBalances;
import de.adorsys.aspsp.xs2a.spi.domain.common.SpiAmount;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountControllerTest {
    private static final String ACCOUNT_ID = "3278921mxl-n2131-13nw-2n123";
    private static final String WRONG_ACCOUNT_ID = "Really wrong id";
    private static final String IBAN = "DE1789232872";
    private static final String WRONG_IBAN = "Wrongest iban ever";

    @MockBean
    private AccountService accountService;
    @Autowired
    private AccountController accountController;
    private List<SpiAccountDetails> accountList = new ArrayList<>();

    @Before
    public void setUpAccountServiceMock() {
        accountList.add(getSpiAccountDetails_1());
        accountList.add(getSpiAccountDetails_2());
        when(accountService.getAccountById(ACCOUNT_ID))
            .thenReturn(Optional.of(getSpiAccountDetails_1()));
        when(accountService.getAccountById(WRONG_ACCOUNT_ID))
            .thenReturn(Optional.empty());
        when(accountService.getAllAccounts(anyString(), anyBoolean()))
            .thenReturn(accountList);
        when(accountService.addAccount(getSpiAccountDetails_1()))
            .thenReturn(Optional.of(getSpiAccountDetails_1()));
        when(accountService.deleteAccountById(ACCOUNT_ID))
            .thenReturn(true);
        when(accountService.deleteAccountById(WRONG_ACCOUNT_ID))
            .thenReturn(false);
        when(accountService.getBalances(ACCOUNT_ID))
            .thenReturn(getNewBalanceList());
        when(accountService.getBalances(WRONG_ACCOUNT_ID))
            .thenReturn(Collections.emptyList());
        when(accountService.getAccountByIban(IBAN, Currency.getInstance("EUR")))
            .thenReturn(Optional.of(getSpiAccountDetails_1()));
        when(accountService.getAccountByIban(WRONG_IBAN, Currency.getInstance("EUR")))
            .thenReturn(Optional.empty());
    }

    @Test
    public void readAllAccounts() {
        //TODO this is a task https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/71
    }

    @Test
    public void readAccountById() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.OK;

        //When:
        ResponseEntity<SpiAccountDetails> actualResponse = accountController.readAccountById(ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        SpiAccountDetails actualResult = actualResponse.getBody();

        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResult).isEqualTo(getSpiAccountDetails_1());
    }

    @Test
    public void readAccountById_wrongId() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.NOT_FOUND;

        //When:
        ResponseEntity<SpiAccountDetails> actualResponse = accountController.readAccountById(WRONG_ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        SpiAccountDetails actualResult = actualResponse.getBody();

        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResult).isNull();
    }

    @Test
    public void readAccountByIban() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.OK;

        //When:
        ResponseEntity<SpiAccountDetails> actualResponse = accountController.readAccountByIban(IBAN, "EUR");

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        SpiAccountDetails actualResult = actualResponse.getBody();

        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResult).isEqualTo(getSpiAccountDetails_1());
    }

    @Test
    public void readAccountByIban_wrongId() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.NOT_FOUND;

        //When:
        ResponseEntity<SpiAccountDetails> actualResponse = accountController.readAccountByIban(WRONG_IBAN, "EUR");

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        SpiAccountDetails actualResult = actualResponse.getBody();

        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResult).isNull();
    }

    @Test
    public void createAccount() throws Exception {
        //Given
        SpiAccountDetails expectedSpiAccountDetails = getSpiAccountDetails_1();
        HttpStatus expectedStatusCode = HttpStatus.CREATED;

        //When
        ResponseEntity actualResponse = accountController.createAccount(expectedSpiAccountDetails);

        //Then
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
    }

    @Test
    public void deleteAccount_Success() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.NO_CONTENT;

        //When:
        ResponseEntity actualResponse = accountController.deleteAccount(ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
    }

    @Test
    public void deleteAccount_WrongId() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.NOT_FOUND;

        //When:
        ResponseEntity actualResponse = accountController.deleteAccount(WRONG_ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
    }

    @Test
    public void readBalancesById() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.OK;
        List<SpiBalances> expectedBalanceList = getNewBalanceList();

        //When:
        ResponseEntity actualResponse = accountController.readBalancesById(ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResponse.getBody()).isEqualTo(expectedBalanceList);
    }

    @Test
    public void readBalancesById_wrongID() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.NOT_FOUND;

        //When:
        ResponseEntity actualResponse = accountController.readBalancesById(WRONG_ACCOUNT_ID);

        //Then:
        HttpStatus actualStatusCode = actualResponse.getStatusCode();
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
        assertThat(actualResponse.getBody()).isNull();
    }

    private SpiAccountDetails getSpiAccountDetails_1() {
        return new SpiAccountDetails(ACCOUNT_ID, IBAN, null, "1111222233334444",
            "111122xxxxxx44", null, Currency.getInstance("EUR"), "Jack", "GIRO",
            null, "XE3DDD", getNewBalanceList());
    }

    private SpiAccountDetails getSpiAccountDetails_2() {
        return new SpiAccountDetails("qwertyuiop12345678", IBAN, null, "4444333322221111",
            "444433xxxxxx1111", null, null, "Emily", "GIRO",
            null, "ACVB222", getNewBalanceList());
    }

    private List<SpiBalances> getNewBalanceList() {
        Currency euro = Currency.getInstance("EUR");

        SpiBalances balance = new SpiBalances();
        balance.setAuthorised(getNewSingleBalances(new SpiAmount(euro, "1000")));
        balance.setOpeningBooked(getNewSingleBalances(new SpiAmount(euro, "200")));

        return Collections.singletonList(balance);
    }

    private SpiAccountBalance getNewSingleBalances(SpiAmount spiAmount) {
        SpiAccountBalance sb = new SpiAccountBalance();
        sb.setDate(new Date(1523951451537L));
        sb.setSpiAmount(spiAmount);
        sb.setLastActionDateTime(new Date(1523951451537L));
        return sb;
    }
}
