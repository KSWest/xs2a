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

import de.adorsys.aspsp.xs2a.domain.ResponseObject;
import de.adorsys.aspsp.xs2a.service.consent.ais.AisConsentService;
import de.adorsys.aspsp.xs2a.service.mapper.AccountMapper;
import de.adorsys.aspsp.xs2a.service.mapper.ConsentMapper;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.consent.AspspConsentData;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.psd2.model.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class ConsentService { //TODO change format of consentRequest to mandatory obtain PSU-Id and only return data which belongs to certain PSU tobe changed upon v1.1

    private final ConsentMapper consentMapper;
    private final AisConsentService aisConsentService;
    private final AccountSpi accountSpi;
    private final AccountMapper accountMapper;

    public static Set<Object> getAccountReferences(AccountAccess accountAccess) {
        return Optional.ofNullable(accountAccess)
            .map(a -> getReferenceSet(accountAccess.getAccounts(), accountAccess.getBalances(), accountAccess.getTransactions()))
            .orElse(Collections.emptySet());
    }

    private static final Set<Object> getReferenceSet(List<Object>... referencesList) {
        return Arrays.stream(referencesList)
            .map(ConsentService::getReferenceList)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static List<Object> getReferenceList(List<Object> reference) {
        return Optional.ofNullable(reference)
            .orElse(Collections.emptyList());
    }

    /**
     * @param request body of create consent request carrying such parameters as AccountAccess, validity terms etc.
     * @param psuId   String representing PSU identification at ASPSP
     * @return CreateConsentResp representing the complete response to create consent request
     * Performs create consent operation either by filling the appropriate AccountAccess fields with corresponding
     * account details or by getting account details from ASPSP by psuId and filling the appropriate fields in
     * AccountAccess determined by availableAccounts or allPsd2 variables
     */
    public ResponseObject<ConsentsResponse201> createAccountConsentsWithResponse(Consents request, String psuId) {
        String tppId = "This is a test TppId"; //TODO v1.1 add corresponding request header
        Consents checkedRequest = new Consents();
        if (isNotEmptyAccess(request.getAccess()) && request.getValidUntil().isAfter(LocalDate.now())) {
            if (isAllAccountsRequest(request) && psuId != null) {
                checkedRequest.setAccess(getAccessByPsuId(AccountAccess.AllPsd2Enum.ALLACCOUNTS == request.getAccess().getAllPsd2(), psuId));
            } else {
                checkedRequest.setAccess(getAccessByRequestedAccess(request.getAccess()));
            }
            checkedRequest.setCombinedServiceIndicator(request.isCombinedServiceIndicator());
            checkedRequest.setRecurringIndicator(request.getRecurringIndicator());
            checkedRequest.setFrequencyPerDay(request.getFrequencyPerDay());
            checkedRequest.setValidUntil(request.getValidUntil());
        }
        String consentId = isNotEmptyAccess(checkedRequest.getAccess())
            ? aisConsentService.createConsent(checkedRequest, psuId, tppId)
            : null;
        //TODO v1.1 Add balances support
        return !StringUtils.isBlank(consentId)
            ? ResponseObject.<ConsentsResponse201>builder().body(new ConsentsResponse201().consentStatus(ConsentStatus.RECEIVED).consentId(consentId)).build()
            : ResponseObject.<ConsentsResponse201>builder()
            .fail(Arrays.asList(new TppMessageGeneric()
                .category(TppMessageCategory.ERROR)
                .code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN)))
            .build();
    }

    /**
     * @param consentId String representation of AccountConsent identification
     * @return ConsentStatus
     * Returns status of requested consent
     */
    public ResponseObject<ConsentStatusResponse200> getAccountConsentsStatusById(String consentId) {
        return consentMapper.mapToConsentStatus(aisConsentService.getAccountConsentStatusById(consentId))
            .map(consentStatus -> new ConsentStatusResponse200().consentStatus(consentStatus))
            .map(status -> ResponseObject.<ConsentStatusResponse200>builder().body(status).build())
            .orElse(ResponseObject.<ConsentStatusResponse200>builder()
                .fail(Arrays.asList(new TppMessageGeneric()
                    .category(TppMessageCategory.ERROR)
                    .code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN)))
                .build());
    }

    /**
     * @param consentId String representation of AccountConsent identification
     * @return VOID
     * Revokes account consent on PSU request
     */
    public ResponseObject<Void> deleteAccountConsentsById(String consentId) {
        if (aisConsentService.getAccountConsentById(consentId) != null) {
            aisConsentService.revokeConsent(consentId);
            return ResponseObject.<Void>builder().build();
        }

        return ResponseObject.<Void>builder()
            .fail(Arrays.asList(new TppMessageGeneric()
                .category(TppMessageCategory.ERROR)
                .code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN)))
            .build();
    }

    /**
     * @param consentId String representation of AccountConsent identification
     * @return AccountConsent requested by consentId
     */
    public ResponseObject<ConsentInformationResponse200Json> getAccountConsentById(String consentId) {
        ConsentInformationResponse200Json consent = consentMapper.mapToAccountConsent(aisConsentService.getAccountConsentById(consentId));
        return consent == null
            ? ResponseObject.<ConsentInformationResponse200Json>builder()
            .fail(Arrays.asList(new TppMessageGeneric()
                .category(TppMessageCategory.ERROR)
                .code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN)))
            .build()
            : ResponseObject.<ConsentInformationResponse200Json>builder().body(consent).build();
    }

    ResponseObject<AccountAccess> getValidatedConsent(String consentId) {
        ConsentInformationResponse200Json consent = consentMapper.mapToAccountConsent(aisConsentService.getAccountConsentById(consentId));
        if (consent == null) {
            return ResponseObject.<AccountAccess>builder()
                .fail(Arrays.asList(new TppMessageGeneric()
                    .category(TppMessageCategory.ERROR)
                    .code(TppMessageGENERICCONSENTUNKNOWN403400.CodeEnum.UNKNOWN)))
                .build();
        }
        if (EnumSet.of(ConsentStatus.VALID, ConsentStatus.RECEIVED).contains(consent.getConsentStatus())) {
            return ResponseObject.<AccountAccess>builder()
                .fail(Arrays.asList(new TppMessageGeneric()
                    .category(TppMessageCategory.ERROR)
                    .code(TppMessageGENERICCONSENTEXPIRED401.CodeEnum.EXPIRED)))
                .build();
        }
        if (consent.getFrequencyPerDay() <= 0) {
            return ResponseObject.<AccountAccess>builder()
                .fail(Arrays.asList(new TppMessageGeneric()
                    .category(TppMessageCategory.ERROR)
                    .code(TppMessageAISACCESSEXCEEDED429.CodeEnum.EXCEEDED)))
                .build();
        }
        return ResponseObject.<AccountAccess>builder().body(consent.getAccess()).build();
    }

    boolean isValidAccountByAccess(String iban, String currency, List<Object> allowedAccountData) {
        //TODO implementation for any kind of account references needed!
        return CollectionUtils.isNotEmpty(allowedAccountData)
            && allowedAccountData.stream()
            .filter(o -> o instanceof AccountReferenceIban)
            .map(o -> (AccountReferenceIban) o)
            .anyMatch(a -> a.getIban().equals(iban)
                && a.getCurrency() == currency);
    }

    private Set<String> getIbansFromAccountReference(List<Object> references) {
        //TODO implementation for any kind of account references needed!
        return Optional.ofNullable(references)
            .map(list -> list.stream()
                .filter(o -> o instanceof AccountReferenceIban)
                .map(o -> (AccountReferenceIban) o)
                .map(AccountReferenceIban::getIban)
                .collect(Collectors.toSet()))
            .orElse(Collections.emptySet());
    }

    private Boolean isNotEmptyAccess(AccountAccess access) {
        return Optional.ofNullable(access)
            .map(this::isNotEmpty)
            .orElse(false);
    }

    private boolean isNotEmpty(AccountAccess access) {
        return !(CollectionUtils.isEmpty(access.getAccounts())
            && CollectionUtils.isEmpty(access.getBalances())
            && CollectionUtils.isEmpty(access.getTransactions())
            && access.getAllPsd2() == null
            && access.getAvailableAccounts() == null);
    }

    private AccountAccess getAccessByRequestedAccess(AccountAccess requestedAccess) {
        Set<String> ibansFromAccess = getIbansFromAccess(requestedAccess);
        List<SpiAccountDetails> accountDetailsList = accountSpi.readAccountDetailsByIbans(
            ibansFromAccess,
            new AspspConsentData("zzzzzzzzzzzzzz".getBytes())).getPayload();
        List<Object> aspspReferences = accountMapper.mapToAccountReferencesFromDetails(accountDetailsList); // TODO https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/191 Put a real data here
        List<Object> balances = getFilteredReferencesByAccessReferences(requestedAccess.getBalances(), aspspReferences);
        List<Object> transaction = getRequestedReferences(requestedAccess.getTransactions(), aspspReferences);
        List<Object> accounts = getRequestedReferences(requestedAccess.getAccounts(), aspspReferences);
        return new AccountAccess().accounts(getAccountsForAccess(balances, transaction, accounts)).balances(balances).transactions(transaction);
    }

    private List<Object> getFilteredReferencesByAccessReferences(List<Object> requestedReferences, List<Object> refs) {
        return Optional.ofNullable(requestedReferences)
            .map(reqRefs -> getRequestedReferences(reqRefs, refs))
            .orElse(Collections.emptyList());
    }

    private List<Object> getAccountsForAccess(List<Object> balances, List<Object> transactions, List<Object> accounts) {
        accounts.removeAll(balances);
        accounts.addAll(balances);
        accounts.removeAll(transactions);
        accounts.addAll(transactions);
        return accounts;
    }

    private List<Object> getRequestedReferences(List<Object> requestedRefs, List<Object> refs) {
        return Optional.ofNullable(requestedRefs).map(rr -> rr.stream()
            .filter(r -> isContainedRefInRefsList(r, refs))
            .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    private boolean isContainedRefInRefsList(Object referenceMatched, List<Object> references) {
        //TODO implementation for any kind of account references needed!
        if (!(referenceMatched instanceof AccountReferenceIban)) {
            return false;
        }

        return references.stream()
            .filter(o -> o instanceof AccountReferenceIban)
            .map(o -> (AccountReferenceIban) o)
            .anyMatch(r -> r.getIban().equals(((AccountReferenceIban) referenceMatched).getIban()));
    }

    private AccountAccess getAccessByPsuId(boolean isAllPSD2, String psuId) {
        List refs = accountMapper.mapToAccountReferencesFromDetails(accountSpi.readAccountsByPsuId(psuId, new AspspConsentData("zzzzzzzzzzzzzz".getBytes())).getPayload()); // TODO https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/191 Put a real data here
        if (CollectionUtils.isNotEmpty(refs)) {
            return isAllPSD2
                ? new AccountAccess().accounts(refs).balances(refs).transactions(refs).allPsd2(AccountAccess.AllPsd2Enum.ALLACCOUNTS)
                : new AccountAccess().accounts(refs).balances(Collections.emptyList()).transactions(Collections.emptyList()).availableAccounts(AccountAccess.AvailableAccountsEnum.ALLACCOUNTS);
        } else {
            return new AccountAccess().accounts(Collections.emptyList()).balances(Collections.emptyList()).transactions(Collections.emptyList());
        }
    }

    private boolean isAllAccountsRequest(Consents request) {
        return Optional.ofNullable(request.getAccess())
            .filter(a -> AccountAccess.AllPsd2Enum.ALLACCOUNTS == a.getAllPsd2()
                || AccountAccess.AvailableAccountsEnum.ALLACCOUNTS == a.getAvailableAccounts()).isPresent();
    }

    private Set<String> getIbansFromAccess(AccountAccess access) {
        return Stream.of(
            getIbansFromAccountReference(access.getAccounts()),
            getIbansFromAccountReference(access.getBalances()),
            getIbansFromAccountReference(access.getTransactions())
        )
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

}
