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

import de.adorsys.aspsp.xs2a.component.JsonConverter;
import de.adorsys.aspsp.xs2a.domain.*;
import de.adorsys.aspsp.xs2a.exception.MessageError;
import de.adorsys.aspsp.xs2a.service.mapper.AccountMapper;
import de.adorsys.aspsp.xs2a.service.validator.ValidationGroup;
import de.adorsys.aspsp.xs2a.service.validator.ValueValidatorService;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiBalances;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.aspsp.xs2a.web.AccountController;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static de.adorsys.aspsp.xs2a.domain.MessageCode.RESOURCE_UNKNOWN_404;
import static de.adorsys.aspsp.xs2a.exception.MessageCategory.ERROR;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Slf4j
@Service
@AllArgsConstructor
public class AccountService {

    private final int maxNumberOfCharInTransactionJson;
    private final AccountSpi accountSpi;
    private final AccountMapper accountMapper;
    private final ValueValidatorService validatorService;
    private final JsonConverter jsonConverter;

    public ResponseObject<Map<String, List<AccountDetails>>> getAccountDetailsList(String consentId, boolean withBalance, boolean psuInvolved) {
        List<AccountDetails> accountDetailsList = accountMapper.mapFromSpiAccountDetailsList(accountSpi.readAccounts(consentId));

        if (!withBalance) {
            accountDetailsList = accountDetailsList.stream()
                                     .map(this::getAccountDetailsWithoutBalances)
                                     .collect(Collectors.toList());
        }

        Map<String, List<AccountDetails>> accountDetailsMap = new HashMap<>();
        accountDetailsMap.put("accountList",accountDetailsList);

        return ResponseObject.builder()
                   .body(accountDetailsMap).build();
    }

    public ResponseObject<List<Balances>> getBalances(String accountId, String consentId, boolean psuInvolved) {
        List<SpiBalances> spiBalances = accountSpi.readBalances(accountId, consentId);

        return Optional.ofNullable(spiBalances)
                   .map(sb -> ResponseObject.builder().body(accountMapper.mapFromSpiBalancesList(sb)).build())
                   .orElse(ResponseObject.builder().fail(new MessageError(new TppMessageInformation(ERROR, RESOURCE_UNKNOWN_404)
                                                                              .text("Wrong account ID"))).build());
    }

    public ResponseObject<AccountReport> getAccountReport(String accountId, String consentId,
                                                          Date dateFrom, Date dateTo, String transactionId,
                                                          boolean psuInvolved, String bookingStatus, boolean withBalance, boolean deltaList) {

        if (accountSpi.readAccountDetails(accountId, consentId) == null) {
            return ResponseObject.builder()
                       .fail(new MessageError(new TppMessageInformation(ERROR, RESOURCE_UNKNOWN_404))).build();
        } else {

            AccountReport accountReport = getAccountReport(accountId, consentId, dateFrom, dateTo, transactionId, psuInvolved, withBalance);
            return ResponseObject.builder()
                       .body(getReportAccordingMaxSize(accountReport, accountId)).build();
        }
    }

    public AccountReport getAccountReportWithDownloadLink(String accountId) {
        // todo further we should implement real flow for downloading file
        String urlToDownload = linkTo(AccountController.class).slash(accountId).slash("transactions/download").toString();
        Links downloadLink = new Links();
        downloadLink.setDownload(urlToDownload);
        return new AccountReport(null, null, downloadLink);
    }

    public AccountDetails getAccountDetails(String accountId, String consentId, boolean withBalance, boolean psuInvolved) {
        AccountDetails accountDetails = accountMapper.mapFromSpiAccountDetails(accountSpi.readAccountDetails(accountId, consentId));
        return withBalance ? accountDetails : getAccountDetailsWithoutBalances(accountDetails);
    }

    private AccountReport getAccountReport(String accountId, String consentId, Date dateFrom, Date dateTo, String transactionId, boolean psuInvolved, boolean withBalance) {
        return StringUtils.isEmpty(transactionId)
                   ? getAccountReportByPeriod(accountId, consentId, dateFrom, dateTo, psuInvolved, withBalance)
                   : getAccountReportByTransaction(accountId, consentId, transactionId, psuInvolved, withBalance);
    }

    private AccountReport getAccountReportByPeriod(String accountId, String consentId, Date dateFrom, Date dateTo, boolean psuInvolved, boolean withBalance) {
        validate_accountId_period(accountId, dateFrom, dateTo);
        return readTransactionsByPeriod(accountId, consentId, dateFrom, dateTo, psuInvolved, withBalance);
    }

    private AccountReport getAccountReportByTransaction(String accountId, String consentId, String transactionId, boolean psuInvolved, boolean withBalance) {
        validate_accountId_transactionId(accountId, transactionId);
        return readTransactionsById(accountId, consentId, transactionId, psuInvolved, withBalance);
    }

    private AccountReport getReportAccordingMaxSize(AccountReport accountReport, String accountId) {
        Optional<String> optionalAccount = jsonConverter.toJson(accountReport);
        String jsonReport = optionalAccount.orElse("");

        if (jsonReport.length() > maxNumberOfCharInTransactionJson) {
            return getAccountReportWithDownloadLink(accountId);
        }

        String urlToAccount = linkTo(AccountController.class).slash(accountId).toString();
        accountReport.get_links().setViewAccount(urlToAccount);
        return accountReport;
    }

    private AccountReport readTransactionsByPeriod(String accountId, String consentId, Date dateFrom,
                                                   Date dateTo, boolean psuInvolved, boolean withBalance) { //NOPMD TODO review and check PMD assertion
        Optional<AccountReport> result = accountMapper.mapFromSpiAccountReport(accountSpi.readTransactionsByPeriod(accountId, consentId, dateFrom, dateTo));

        return result.orElseGet(() -> new AccountReport(new Transactions[]{}, new Transactions[]{}, new Links()));
    }

    private AccountReport readTransactionsById(String accountId, String consentId, String transactionId,
                                               boolean psuInvolved, boolean withBalance) { //NOPMD TODO review and check PMD assertion
        Optional<AccountReport> result = accountMapper.mapFromSpiAccountReport(accountSpi.readTransactionsById(accountId, consentId, transactionId));

        return result.orElseGet(() -> new AccountReport(new Transactions[]{},
            new Transactions[]{},
            new Links()
        ));
    }

    private AccountDetails getAccountDetailsWithoutBalances(AccountDetails accountDetails) {
        return new AccountDetails(accountDetails.getId(), accountDetails.getIban(), accountDetails.getBban(), accountDetails.getPan(),
            accountDetails.getMaskedPan(), accountDetails.getMsisdn(), accountDetails.getCurrency(), accountDetails.getName(),
            accountDetails.getAccountType(), accountDetails.getCashAccountType(), accountDetails.getBic(), null, null);
    }

    // Validation
    private void validate_accountId_period(String accountId, Date dateFrom, Date dateTo) {
        ValidationGroup fieldValidator = new ValidationGroup();
        fieldValidator.setAccountId(accountId);
        fieldValidator.setDateFrom(dateFrom);
        fieldValidator.setDateTo(dateTo);

        validatorService.validate(fieldValidator, ValidationGroup.AccountIdAndPeriodIsValid.class);
    }

    private void validate_accountId_transactionId(String accountId, String transactionId) {
        ValidationGroup fieldValidator = new ValidationGroup();
        fieldValidator.setAccountId(accountId);
        fieldValidator.setTransactionId(transactionId);

        validatorService.validate(fieldValidator, ValidationGroup.AccountIdAndTransactionIdIsValid.class);
    }
}
