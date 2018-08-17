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

package de.adorsys.aspsp.xs2a.service;

import de.adorsys.aspsp.xs2a.consent.api.TypeAccess;
import de.adorsys.aspsp.xs2a.domain.BookingStatus;
import de.adorsys.aspsp.xs2a.domain.ResponseObject;
import de.adorsys.aspsp.xs2a.domain.Transactions;
import de.adorsys.aspsp.xs2a.service.consent.ais.AisConsentService;
import de.adorsys.aspsp.xs2a.service.mapper.AccountMapper;
import de.adorsys.aspsp.xs2a.service.validator.ValueValidatorService;
import de.adorsys.aspsp.xs2a.spi.domain.SpiResponse;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountBalance;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiBalanceType;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiTransaction;
import de.adorsys.aspsp.xs2a.spi.domain.common.SpiAmount;
import de.adorsys.aspsp.xs2a.spi.domain.consent.AspspConsentData;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.psd2.model.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {
    private final String ACCOUNT_ID = "33333-999999999";
    private final String ACCOUNT_ID_1 = "77777-999999999";
    private final String WRONG_ACCOUNT_ID = "Wrong account";
    private final String IBAN = "DE123456789";
    private final String IBAN_1 = "DE987654321";
    private final Currency CURRENCY = Currency.getInstance("EUR");
    private final Currency CURRENCY_1 = Currency.getInstance("USD");
    private final String CONSENT_ID_WB = "111222333";
    private final String CONSENT_ID_WOB = "333222111";
    private final String CONSENT_ID_WT = "777999777";
    private final String WRONG_CONSENT_ID = "Wromg consent id";
    private final String TRANSACTION_ID = "0001";
    private final LocalDate DATE = LocalDate.parse("2019-03-03");
    private final AspspConsentData ASPSP_CONSENT_DATA = new AspspConsentData("zzzzzzzzzzzzzz".getBytes());

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountSpi accountSpi;
    @Mock
    private ConsentService consentService;
    @Mock
    private AisConsentService aisConsentService;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private ValueValidatorService valueValidatorService;

    @Before
    public void setUp() {
        //Validation
        doNothing().when(valueValidatorService).validate(any(), any());
        //AccountMapping
        when(accountMapper.mapToAccountDetails(getSpiAccountDetails(ACCOUNT_ID, IBAN))).thenReturn(getAccountDetails(ACCOUNT_ID, IBAN));
        when(accountMapper.mapToAccountDetails(getSpiAccountDetails(ACCOUNT_ID_1, IBAN_1))).thenReturn(getAccountDetails(ACCOUNT_ID_1, IBAN_1));
        when(accountMapper.mapToAccountDetails(null)).thenReturn(null);
        when(accountMapper.mapToAccountReport(Collections.singletonList(getSpiTransaction()))).thenReturn(Optional.of(getReport()));
        //AisReporting
        doNothing().when(aisConsentService).consentActionLog(anyString(), anyString(), anyBoolean(), any(TypeAccess.class), any(ResponseObject.class));
        //getAccountDetailsByAccountId_WoB_Success
        when(accountSpi.readAccountDetails(ACCOUNT_ID, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(getSpiAccountDetails(ACCOUNT_ID, IBAN), ASPSP_CONSENT_DATA));
        when(consentService.getValidatedConsent(CONSENT_ID_WOB)).thenReturn(getAccessResponse(getReferences(IBAN, IBAN_1), null, null, false, false));
        when(consentService.isValidAccountByAccess(IBAN, CURRENCY.getCurrencyCode(), getReferences(IBAN, IBAN_1))).thenReturn(true);
        //getAccountDetailsByAccountId_WB_Success
        when(consentService.getValidatedConsent(CONSENT_ID_WB)).thenReturn(getAccessResponse(getReferences(IBAN, IBAN_1), getReferences(IBAN, IBAN_1), null, false, false));
        when(consentService.isValidAccountByAccess(IBAN, CURRENCY.getCurrencyCode(), getReferences(IBAN, IBAN_1))).thenReturn(true);
        //getAccountDetailsByAccountId_Failure_wrongAccount
        when(accountSpi.readAccountDetails(WRONG_ACCOUNT_ID, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(null, ASPSP_CONSENT_DATA));
        //getAccountDetailsByAccountId_Failure_wrongConsent
        when(consentService.getValidatedConsent(WRONG_CONSENT_ID)).thenReturn(ResponseObject.<AccountAccess>builder().fail(Arrays.asList(new TppMessageGeneric().category(TppMessageCategory.ERROR).code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN))).build());

        //getAccountDetailsListByConsent_Success
        when(accountSpi.readAccountDetailsByIban(IBAN, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(Collections.singletonList(getSpiAccountDetails(ACCOUNT_ID, IBAN)), ASPSP_CONSENT_DATA));
        when(accountSpi.readAccountDetailsByIban(IBAN_1, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(Collections.singletonList(getSpiAccountDetails(ACCOUNT_ID_1, IBAN_1)), ASPSP_CONSENT_DATA));

        when(accountSpi.readAccountDetails(WRONG_ACCOUNT_ID, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(null, ASPSP_CONSENT_DATA));

        //getAccountReport_ByTransactionId_Success
        when(consentService.getValidatedConsent(CONSENT_ID_WT)).thenReturn(getAccessResponse(getReferences(IBAN, IBAN_1), null, getReferences(IBAN, IBAN_1), false, false));
        when(accountSpi.readTransactionById(TRANSACTION_ID, ACCOUNT_ID, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(Optional.of(getSpiTransaction()), ASPSP_CONSENT_DATA));

        when(accountSpi.readTransactionsByPeriod(ACCOUNT_ID, DATE, DATE, ASPSP_CONSENT_DATA)).thenReturn(new SpiResponse<>(Collections.singletonList(getSpiTransaction()), ASPSP_CONSENT_DATA));
    }

    //Get Account By AccountId
    @Test
    public void getAccountDetailsByAccountId_WoB_Success() {
        //When:
        ResponseObject<AccountDetails> response = accountService.getAccountDetails(CONSENT_ID_WOB, ACCOUNT_ID, false);

        //Then:
        assertThat(response.getBody().getResourceId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getBody().getBalances()).isEqualTo(null);
    }

    @Test
    public void getAccountDetailsByAccountId_WB_Success() {
        //When:
        ResponseObject<AccountDetails> response = accountService.getAccountDetails(CONSENT_ID_WB, ACCOUNT_ID, true);

        //Then:
        assertThat(response.getBody().getResourceId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getBody().getBalances()).isEqualTo(getBalancesList());
    }

    @Test
    public void getAccountDetailsByAccountId_Failure_wrongAccount() {
        //When:
        ResponseObject<AccountDetails> response = accountService.getAccountDetails(CONSENT_ID_WB, WRONG_ACCOUNT_ID, true);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(RESOURCE_UNKNOWN_404);
    }

    @Test
    public void getAccountDetailsByAccountId_Failure_wrongConsent() {
        //When:
        ResponseObject<AccountDetails> response = accountService.getAccountDetails(WRONG_CONSENT_ID, ACCOUNT_ID, true);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_UNKNOWN_403);
    }

    //Get AccountsList By Consent
    @Test
    public void getAccountDetailsListByConsent_Success_WOB() {
        //When:
        ResponseObject<Map<String, List<AccountDetails>>> response = accountService.getAccountDetailsList(CONSENT_ID_WOB, false);

        //Then:
        assertThat(response.getBody().get("accountList").get(0).getResourceId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getBody().get("accountList").get(1).getResourceId()).isEqualTo(ACCOUNT_ID_1);
        assertThat(response.getBody().get("accountList").get(0).getBalances()).isEqualTo(null);
        assertThat(response.getBody().get("accountList").get(1).getBalances()).isEqualTo(null);
        assertThat(response.getBody().get("accountList").get(0).getLinks()).isEqualTo(new HashMap<>());
        assertThat(response.getBody().get("accountList").get(1).getLinks()).isEqualTo(new HashMap());
    }

    @Test
    public void getAccountDetailsListByConsent_Success_WB() {
        //When:
        ResponseObject<Map<String, List<AccountDetails>>> response = accountService.getAccountDetailsList(CONSENT_ID_WB, true);

        //Then:
        assertThat(response.getBody().get("accountList").get(0).getResourceId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getBody().get("accountList").get(1).getResourceId()).isEqualTo(ACCOUNT_ID_1);
        assertThat(response.getBody().get("accountList").get(0).getBalances()).isEqualTo(getBalancesList());
        assertThat(response.getBody().get("accountList").get(1).getBalances()).isEqualTo(getBalancesList());
        assertThat(response.getBody().get("accountList").get(0).getLinks()).isEqualTo(getAccountDetails(ACCOUNT_ID, IBAN).getLinks());
        assertThat(response.getBody().get("accountList").get(1).getLinks()).isEqualTo(getAccountDetails(ACCOUNT_ID_1, IBAN_1).getLinks());
    }

    @Test
    public void getAccountDetailsListByConsent_Failure_Wrong_Consent() {
        //When:
        ResponseObject<Map<String, List<AccountDetails>>> response = accountService.getAccountDetailsList(WRONG_CONSENT_ID, false);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_UNKNOWN_403);
    }

    //Get Balances
    @Test
    public void getBalances_Success_Consent_WB() {
        //When:
        ResponseObject<List<Balance>> response = accountService.getBalances(CONSENT_ID_WB, ACCOUNT_ID);

        //Then:
        assertThat(response.getBody()).isEqualTo(getBalancesList());
    }

    @Test
    public void getBalances_Failure_Consent_WOB() {
        //When:
        ResponseObject<List<Balance>> response = accountService.getBalances(CONSENT_ID_WOB, ACCOUNT_ID);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_INVALID);
    }

    @Test
    public void getBalances_Failure_Wrong_Consent() {
        //When:
        ResponseObject<List<Balance>> response = accountService.getBalances(WRONG_CONSENT_ID, ACCOUNT_ID);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_UNKNOWN_403);
    }

    @Test
    public void getBalances_Failure_Wrong_Account() {
        //When:
        ResponseObject<List<Balance>> response = accountService.getBalances(CONSENT_ID_WB, WRONG_ACCOUNT_ID);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(RESOURCE_UNKNOWN_404);
    }

    //Get Transaction By TransactionId
    @Test
    public void getAccountReport_ByTransactionId_Success() {
        //When:
        ResponseObject<AccountReport> response = accountService.getAccountReport(CONSENT_ID_WT, ACCOUNT_ID, null, null, TRANSACTION_ID, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.getError()).isEqualTo(null);
        assertThat(response.getBody().getBooked().get(0).getTransactionId()).isEqualTo(getTransaction().getTransactionId());
    }

    @Test
    public void getAccountReport_ByTransactionId_WrongConsent_Failure() {
        //When:
        ResponseObject response = accountService.getAccountReport(WRONG_CONSENT_ID, ACCOUNT_ID, null, null, TRANSACTION_ID, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_UNKNOWN_403);
    }

    @Test
    public void getAccountReport_ByTransactionId_AccountMismatch_Failure() {
        //When:
        ResponseObject response = accountService.getAccountReport(CONSENT_ID_WOB, WRONG_ACCOUNT_ID, null, null, TRANSACTION_ID, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(RESOURCE_UNKNOWN_404);
    }

    //Get Transactions By Period
    @Test
    public void getAccountReport_ByPeriod_Success() {
        //When:
        ResponseObject<AccountReport> response = accountService.getAccountReport(CONSENT_ID_WT, ACCOUNT_ID, DATE, DATE, null, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.getError()).isEqualTo(null);
        assertThat(response.getBody().getBooked().get(0).getTransactionId()).isEqualTo(getTransaction().getTransactionId());
    }

    @Test
    public void getAccountReport_ByPeriod_Failure_Wrong_Account() {
        //When:
        ResponseObject response = accountService.getAccountReport(CONSENT_ID_WB, WRONG_ACCOUNT_ID, DATE, DATE, null, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(RESOURCE_UNKNOWN_404);
    }

    @Test
    public void getAccountReport_ByPeriod_Failure_Wrong_Consent() {
        //When:
        ResponseObject response = accountService.getAccountReport(WRONG_CONSENT_ID, ACCOUNT_ID, DATE, DATE, null, false, BookingStatus.BOTH, false, false);

        //Then:
        assertThat(response.hasError()).isEqualTo(true);
        //TODO fix it, errors is a list
//        assertThat(response.getError().getTransactionStatus()).isEqualTo(TransactionStatus.RJCT);
//        assertThat(response.getError().getTppMessage().getCode()).isEqualTo(CONSENT_UNKNOWN_403);
    }

    //Test Stuff
    private ResponseObject<AccountAccess> getAccessResponse(List<Object> accounts, List<Object> balances, List<Object> transactions, boolean allAccounts, boolean allPsd2) {
        return ResponseObject.<AccountAccess>builder().body(getAccessForMock(accounts, balances, transactions, allAccounts, allPsd2)).build();
    }

    private AccountAccess getAccessForMock(List<Object> accounts, List<Object> balances, List<Object> transactions, boolean allAccounts, boolean allPsd2) {
        return new AccountAccess()
            .accounts(accounts)
            .balances(balances)
            .transactions(transactions)
            .availableAccounts(allAccounts ? AccountAccess.AvailableAccountsEnum.ALLACCOUNTS : null)
            .allPsd2(allPsd2 ? AccountAccess.AllPsd2Enum.ALLACCOUNTS : null);
    }

    private Object getAccountReference() {
        AccountDetails details = getAccountDetails(ACCOUNT_ID, IBAN);

        if (StringUtils.isNotEmpty(details.getIban())) {
            return new AccountReferenceIban().iban(details.getIban()).currency(details.getCurrency());
        } else if (StringUtils.isNotEmpty(details.getBban())) {
            return new AccountReferenceBban().bban(details.getBban()).currency(details.getCurrency());
        } else if (StringUtils.isNotEmpty(details.getMsisdn())) {
            return new AccountReferencePan().pan(details.getMsisdn()).currency(details.getCurrency());
        }
        return null;
    }

    private AccountDetails getAccountDetails(String accountId, String iban) {
        return new AccountDetails().resourceId(accountId)
            .iban(iban)
            .currency(CURRENCY.getCurrencyCode())
            .balances(getBalancesList())
            .name("David Muller");
    }

    private BalanceList getBalancesList() {
        BalanceList balances = new BalanceList();
        Balance sb = new Balance();
        Amount amount = new Amount();
        amount.setCurrency(CURRENCY.getCurrencyCode());
        amount.setAmount("1000");
        sb.setBalanceAmount(amount);

        balances.add(sb);
        return balances;
    }

    private SpiAccountDetails getSpiAccountDetails(String accountId, String iban) {
        return new SpiAccountDetails(
            accountId,
            iban,
            "zz22",
            null,
            null,
            null,
            iban.equals(IBAN)
                ? CURRENCY
                : CURRENCY_1,
            "David Muller",
            null,
            null,
            null,
            getSpiBalances());
    }

    private List<SpiAccountBalance> getSpiBalances() {
        SpiAccountBalance sb = new SpiAccountBalance();
        SpiAmount amount = new SpiAmount(CURRENCY.getCurrencyCode(), BigDecimal.valueOf(1000));
        sb.setSpiBalanceAmount(amount);
        sb.setSpiBalanceType(SpiBalanceType.INTERIM_AVAILABLE);
        return Collections.singletonList(sb);
    }

    private Transactions getTransaction() {
        Transactions transaction = new Transactions();
        transaction.setTransactionId(TRANSACTION_ID);
        transaction.setBookingDate(DATE);
        transaction.setValueDate(DATE);
        //TODO transaction.setCreditorAccount(getAccountReference());
        Amount amount = new Amount();
        amount.setAmount("1000");
        amount.setCurrency(CURRENCY.getCurrencyCode());
        //TODO  transaction.setAmount(amount);
        return transaction;
    }

    private SpiTransaction getSpiTransaction() {
        Transactions t = getTransaction();
        return null;
        //TODO payment 1.2 spec refactoring needed
//        return new SpiTransaction(t.getTransactionId(), null, null, null, t.getBookingDate(),
//            t.getValueDate(), new SpiAmount(t.getAmount().getCurrency().getCurrencyCode(), new BigDecimal(t.getAmount().getContent())), null,
//            mapToSpiAccountRef(t.getCreditorAccount()), null, null,
//            mapToSpiAccountRef(t.getDebtorAccount()), null, null,
//            null, null, null);
    }

//    private SpiAccountReference mapToSpiAccountRef(AccountReference reference) {
//        return Optional.ofNullable(reference).map(r -> new SpiAccountReference(r.getIban(), r.getBban(), r.getPan(),
//            r.getMaskedPan(), r.getMsisdn(), r.getCurrency())).orElse(null);
//    }

    private List<Object> getReferences(String iban, String iban1) {
        return Arrays.asList(getReference(iban), getReference(iban1));
    }

    private Object getReference(String iban) {
        AccountReferenceIban reference = new AccountReferenceIban();
        reference.setIban(iban);
        reference.setCurrency(iban.equals(IBAN) ? CURRENCY.getCurrencyCode() : CURRENCY_1.getCurrencyCode());
        return reference;
    }

    private AccountReport getReport() {
        return new AccountReport().booked(new TransactionList()).pending(new TransactionList());
    }
}
