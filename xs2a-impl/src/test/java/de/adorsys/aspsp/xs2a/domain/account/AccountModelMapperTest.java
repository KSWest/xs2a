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

package de.adorsys.aspsp.xs2a.domain.account;

import de.adorsys.aspsp.xs2a.domain.*;
import de.adorsys.aspsp.xs2a.domain.Amount;
import de.adorsys.aspsp.xs2a.domain.Balance;
import de.adorsys.aspsp.xs2a.domain.BalanceType;
import de.adorsys.aspsp.xs2a.domain.CashAccountType;
import de.adorsys.aspsp.xs2a.domain.code.BankTransactionCode;
import de.adorsys.aspsp.xs2a.domain.code.PurposeCode;
import de.adorsys.psd2.model.*;
import org.junit.Test;

import java.time.*;
import java.util.*;

import static de.adorsys.aspsp.xs2a.service.mapper.AccountModelMapper.*;
import static org.junit.Assert.*;

public class AccountModelMapperTest {

    @Test
    public void testBalanceMapping() {
        Balance balance = createBalance();

        de.adorsys.psd2.model.Balance result = mapToBalance(balance);
        assertNotNull(result);

        Amount expectedBalanceAmount = balance.getBalanceAmount();
        de.adorsys.psd2.model.Amount actualBalanceAmount = result.getBalanceAmount();
        assertNotNull(expectedBalanceAmount);

        assertEquals(expectedBalanceAmount.getContent(), actualBalanceAmount.getAmount());
        assertEquals(expectedBalanceAmount.getCurrency().getCurrencyCode(), actualBalanceAmount.getCurrency());
        assertEquals(balance.getBalanceType().name(), result.getBalanceType().name());

        LocalDateTime expectedLastChangeDateTime = balance.getLastChangeDateTime();
        assertNotNull(expectedLastChangeDateTime);

        OffsetDateTime actualLastChangeDateTime = result.getLastChangeDateTime();
        assertNotNull(actualLastChangeDateTime);

        List<ZoneOffset> validOffsets = ZoneId.systemDefault().getRules().getValidOffsets(expectedLastChangeDateTime); //TODO remove when OffsetDateTime is in xs2a
        assertEquals(expectedLastChangeDateTime, actualLastChangeDateTime.atZoneSameInstant(validOffsets.get(0)).toLocalDateTime());
        assertEquals(balance.getLastCommittedTransaction(), result.getLastCommittedTransaction());
        assertEquals(balance.getReferenceDate(), result.getReferenceDate());
    }

    @Test
    public void testMapToAccountList() {
        List<AccountDetails> accountDetailsList = new ArrayList<>();
        Balance inputBalance = createBalance();

        accountDetailsList.add(new AccountDetails("1", "2", "3", "4", "5", "6", Currency.getInstance("EUR"), "8", "9", CashAccountType.CURRENT_ACCOUNT, "11", null, null, new ArrayList<Balance>()));
        accountDetailsList.add(new AccountDetails("x1", "x2", "x3", "x4", "x5", "x6", Currency.getInstance("EUR"), "x8", "x9", CashAccountType.CURRENT_ACCOUNT, "x11", null, null, Arrays.asList(inputBalance)));
        AccountDetails accountDetails = new AccountDetails("y1", "y2", "y3", "y4", "y5", "y6", Currency.getInstance("EUR"), "y8", "y9", CashAccountType.CURRENT_ACCOUNT, "y11", null, null, new ArrayList<Balance>());
        accountDetails.setLinks(createLinks());
        accountDetailsList.add(accountDetails);
        Map<String, List<AccountDetails>> accountDetailsMap = Collections.singletonMap("TEST", accountDetailsList);

        AccountList result = mapToAccountList(accountDetailsMap);
        assertNotNull(result);

        List<de.adorsys.psd2.model.AccountDetails> accounts = result.getAccounts();
        assertNotNull(accounts);

        de.adorsys.psd2.model.AccountDetails secondAccount = accounts.get(1);
        assertNotNull(secondAccount);

        assertEquals("x2", secondAccount.getIban());

        BalanceList balances = secondAccount.getBalances();
        assertNotNull(balances);

        de.adorsys.psd2.model.Balance balance = balances.get(0);
        assertNotNull(balance);

        assertEquals("4711", balance.getLastCommittedTransaction());

        de.adorsys.psd2.model.Amount balanceAmount = balance.getBalanceAmount();
        assertNotNull(balanceAmount);

        assertEquals("EUR", balanceAmount.getCurrency());
        assertEquals("1000", balanceAmount.getAmount());

        LocalDateTime expectedLastChangeDateTime = inputBalance.getLastChangeDateTime();
        assertNotNull(expectedLastChangeDateTime);

        OffsetDateTime actualLastChangeDateTime = balance.getLastChangeDateTime();
        assertNotNull(actualLastChangeDateTime);

        List<ZoneOffset> validOffsets = ZoneId.systemDefault().getRules().getValidOffsets(expectedLastChangeDateTime); //TODO remove when OffsetDateTime is in xs2a
        assertEquals(expectedLastChangeDateTime, actualLastChangeDateTime.atZoneSameInstant(validOffsets.get(0)).toLocalDateTime());

        de.adorsys.psd2.model.AccountDetails thirdAccount = accounts.get(2);
        assertNotNull(thirdAccount);

        Map links = thirdAccount.getLinks();
        assertNotNull(links);

        assertEquals("http://scaOAuth.xx", links.get("scaOAuth"));
        assertEquals("http://linkToStatus.xx", links.get("status"));
        assertEquals("http://linkToSelf.xx", links.get("self"));
    }

    @Test
    public void testMapToTransaction() {
        Transactions transactions = createTransactions();
        TransactionDetails transactionDetails = mapToTransaction(transactions);
        assertNotNull(transactionDetails);

        Amount amount = transactions.getAmount();
        de.adorsys.psd2.model.Amount amountTarget = transactionDetails.getTransactionAmount();
        assertNotNull(amountTarget);

        assertEquals(amount.getContent(), amountTarget.getAmount());
        assertEquals(amount.getCurrency().getCurrencyCode(), amountTarget.getCurrency());

        BankTransactionCode bankTransactionCodeCode = transactions.getBankTransactionCodeCode();
        assertNotNull(bankTransactionCodeCode);

        assertEquals(bankTransactionCodeCode.getCode(), transactionDetails.getBankTransactionCode());
        assertEquals(transactions.getBookingDate(), transactionDetails.getBookingDate());

        AccountReference expectedCreditorAccount = transactions.getCreditorAccount();
        assertNotNull(expectedCreditorAccount);

        AccountReferenceIban actualCreditorAccount = (AccountReferenceIban) transactionDetails.getCreditorAccount();
        assertNotNull(actualCreditorAccount);

        assertEquals(expectedCreditorAccount.getIban(), actualCreditorAccount.getIban());
        assertEquals(expectedCreditorAccount.getCurrency().getCurrencyCode(), actualCreditorAccount.getCurrency());
        assertEquals(transactions.getCreditorId(), transactionDetails.getCreditorId());
        assertEquals(transactions.getCreditorName(), transactionDetails.getCreditorName());

        AccountReference expectedDebtorAccount = transactions.getDebtorAccount();
        assertNotNull(expectedDebtorAccount);

        AccountReferenceIban actualDebtorAccount = (AccountReferenceIban) transactionDetails.getDebtorAccount();
        assertNotNull(actualDebtorAccount);

        assertEquals(expectedDebtorAccount.getIban(), actualDebtorAccount.getIban());
        assertEquals(expectedDebtorAccount.getCurrency().getCurrencyCode(), actualDebtorAccount.getCurrency());

        PurposeCode expectedPurposeCode = transactions.getPurposeCode();
        assertNotNull(expectedPurposeCode);

        de.adorsys.psd2.model.PurposeCode actualPurposeCode = transactionDetails.getPurposeCode();
        assertNotNull(actualPurposeCode);

        assertEquals(expectedPurposeCode.getCode(), actualPurposeCode.name());
        assertEquals(transactions.getRemittanceInformationStructured(), transactionDetails.getRemittanceInformationStructured());
        assertEquals(transactions.getRemittanceInformationUnstructured(), transactionDetails.getRemittanceInformationUnstructured());
        assertEquals(transactions.getTransactionId(), transactionDetails.getTransactionId());
        assertEquals(transactions.getUltimateCreditor(), transactionDetails.getUltimateCreditor());
        assertEquals(transactions.getUltimateDebtor(), transactionDetails.getUltimateDebtor());
        assertEquals(transactions.getValueDate(), transactionDetails.getValueDate());
    }

    @Test
    public void testMapToAccountReport() {
        Transactions [] bookedTransactions = { createTransactions(), createTransactions(), createTransactions()};
        Transactions [] pendingTransactions = { createTransactions(), createTransactions()};
        AccountReport accountReport = new AccountReport(bookedTransactions, pendingTransactions);
        accountReport.setLinks(createLinks());

        de.adorsys.psd2.model.AccountReport result = mapToAccountReport(accountReport);
        assertNotNull(result);

        //transactions mapping tested in testMapToTransaction
        assertEquals(accountReport.getBooked().length, result.getBooked().size());

        Transactions[] expectedPending = accountReport.getPending();
        assertNotNull(expectedPending);

        TransactionList actualPending = result.getPending();
        assertNotNull(actualPending);

        assertEquals(expectedPending.length, actualPending.size());

        Map links = result.getLinks();
        assertEquals(accountReport.getLinks().getScaOAuth(), links.get("scaOAuth"));
        assertEquals(3, links.size());
    }

    private Balance createBalance() {
        Balance balance = new Balance();

        Amount amount = createAmount();

        balance.setBalanceAmount(amount);
        balance.setBalanceType(BalanceType.AUTHORISED);
        balance.setLastChangeDateTime(LocalDateTime.now());
        balance.setLastCommittedTransaction("4711");
        balance.setReferenceDate(LocalDate.now());
        return balance;
    }

    private Amount createAmount() {
        Amount amount = new Amount();
        amount.setCurrency(Currency.getInstance("EUR"));
        amount.setContent("1000");
        return amount;
    }

    private AccountReference createAccountReference() {
        AccountReference accountReference = new AccountReference();
        accountReference.setBban("bban");
        accountReference.setCurrency(Currency.getInstance("EUR"));
        accountReference.setIban("DE1234");
        accountReference.setMaskedPan("maskedPan");
        accountReference.setMsisdn("msisdn");
        accountReference.setPan("pan");
        return accountReference;
    }

    private Transactions createTransactions() {
        Transactions transactions = new Transactions();
        Amount amount = createAmount();
        transactions.setAmount(amount);
        transactions.setBankTransactionCodeCode(new BankTransactionCode("code"));
        transactions.setBookingDate(LocalDate.now());
        transactions.setCreditorAccount(createAccountReference());
        transactions.setCreditorId("creditorId");
        transactions.setCreditorName("Creditor Name");
        transactions.setDebtorAccount(createAccountReference());
        transactions.setDebtorName("Debtor Name");
        transactions.setEndToEndId("endToEndId");
        transactions.setMandateId("mandateId");
        transactions.setPurposeCode(new PurposeCode("BKDF"));
        transactions.setRemittanceInformationStructured("setRemittanceInformationStructured");
        transactions.setRemittanceInformationUnstructured("setRemittanceInformationUnstructured");
        transactions.setTransactionId("transactionId");
        transactions.setUltimateCreditor("ultimateCreditor");
        transactions.setUltimateDebtor("ultimateDebtor");
        transactions.setValueDate(LocalDate.now());
        return transactions;
    }

    private Links createLinks() {
        Links links = new Links();
        links.setScaOAuth("http://scaOAuth.xx");
        links.setStatus("http://linkToStatus.xx");
        links.setSelf("http://linkToSelf.xx");
        return links;
    }
}
