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

package de.adorsys.aspsp.xs2a.service.mapper.consent;

import de.adorsys.aspsp.xs2a.consent.api.*;
import de.adorsys.aspsp.xs2a.consent.api.ais.*;
import de.adorsys.aspsp.xs2a.domain.MessageErrorCode;
import de.adorsys.aspsp.xs2a.domain.account.AccountReference;
import de.adorsys.aspsp.xs2a.domain.consent.*;
import de.adorsys.aspsp.xs2a.service.mapper.AccountMapper;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountConsentAuthorization;
import de.adorsys.aspsp.xs2a.spi.domain.consent.*;
import de.adorsys.psd2.model.ScaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Xs2aAisConsentMapper {
    private final AccountMapper accountMapper;

    public CreateAisConsentRequest mapToCreateAisConsentRequest(CreateConsentReq req, String psuId, String tppId, AspspConsentData aspspConsentData) {
        return Optional.ofNullable(req)
                   .map(r -> {
                       CreateAisConsentRequest aisRequest = new CreateAisConsentRequest();
                       aisRequest.setPsuId(psuId);
                       aisRequest.setTppId(tppId);
                       aisRequest.setFrequencyPerDay(r.getFrequencyPerDay());
                       aisRequest.setAccess(mapToAisAccountAccessInfo(req.getAccess()));
                       aisRequest.setValidUntil(r.getValidUntil());
                       aisRequest.setRecurringIndicator(r.isRecurringIndicator());
                       aisRequest.setCombinedServiceIndicator(r.isCombinedServiceIndicator());
                       aisRequest.setAspspConsentData(aspspConsentData.getAspspConsentData());

                       return aisRequest;
                   })
                   .orElse(null);
    }

    public AccountConsent mapToAccountConsent(SpiAccountConsent spiAccountConsent) {
        return Optional.ofNullable(spiAccountConsent)
                   .map(ac -> new AccountConsent(
                       ac.getId(),
                       mapToAccountAccess(ac.getAccess()),
                       ac.isRecurringIndicator(),
                       ac.getValidUntil(),
                       ac.getFrequencyPerDay(),
                       ac.getLastActionDate(),
                       ConsentStatus.valueOf(ac.getConsentStatus().name()),
                       ac.isWithBalance(),
                       ac.isTppRedirectPreferred()))
                   .orElse(null);
    }

    public Optional<ConsentStatus> mapToConsentStatus(SpiConsentStatus spiConsentStatus) {
        return Optional.ofNullable(spiConsentStatus)
                   .map(status -> ConsentStatus.valueOf(status.name()));
    }

    public ActionStatus mapActionStatusError(MessageErrorCode error, boolean withBalance, TypeAccess access) {
        ActionStatus actionStatus = ActionStatus.FAILURE_ACCOUNT;
        if (error == MessageErrorCode.ACCESS_EXCEEDED) {
            actionStatus = ActionStatus.CONSENT_LIMIT_EXCEEDED;
        } else if (error == MessageErrorCode.CONSENT_EXPIRED) {
            actionStatus = ActionStatus.CONSENT_INVALID_STATUS;
        } else if (error == MessageErrorCode.CONSENT_UNKNOWN_400) {
            actionStatus = ActionStatus.CONSENT_NOT_FOUND;
        } else if (error == MessageErrorCode.CONSENT_INVALID) {
            if (access == TypeAccess.TRANSACTION) {
                actionStatus = ActionStatus.FAILURE_TRANSACTION;
            } else if (access == TypeAccess.BALANCE || withBalance) {
                actionStatus = ActionStatus.FAILURE_BALANCE;
            }
        }
        return actionStatus;
    }

    public SpiUpdateConsentPsuDataReq mapToSpiUpdateConsentPsuDataReq(UpdateConsentPsuDataResponse updatePsuData) {
        return Optional.ofNullable(updatePsuData)
                   .map(data -> {
                       SpiUpdateConsentPsuDataReq request = new SpiUpdateConsentPsuDataReq();
                       request.setPsuId(updatePsuData.getPsuId());
                       request.setConsentId(updatePsuData.getConsentId());
                       request.setAuthorizationId(updatePsuData.getAuthorizationId());
                       request.setAuthenticationMethodId(updatePsuData.getAuthenticationMethodId());
                       request.setScaAuthenticationData(updatePsuData.getScaAuthenticationData());
                       request.setPassword(updatePsuData.getPassword());
                       return request;
                   })
                   .orElse(null);
    }

    public Optional<SpiConsentStatus> mapToSpiConsentStatus(CmsConsentStatus consentStatus) {
        return Optional.ofNullable(consentStatus)
                   .map(status -> SpiConsentStatus.valueOf(status.name()));
    }

    public AccountConsentAuthorization mapToAccountConsentAuthorization(SpiAccountConsentAuthorization spiConsentAuthorization) {
        return Optional.ofNullable(spiConsentAuthorization)
                   .map(conAuth -> {
                       AccountConsentAuthorization consentAuthorization = new AccountConsentAuthorization();

                       consentAuthorization.setId(conAuth.getId());
                       consentAuthorization.setConsentId(conAuth.getConsentId());
                       consentAuthorization.setPsuId(conAuth.getPsuId());
                       consentAuthorization.setScaStatus(ScaStatus.valueOf(conAuth.getScaStatus().name()));
                       consentAuthorization.setAuthenticationMethodId(conAuth.getAuthenticationMethodId());
                       consentAuthorization.setScaAuthenticationData(conAuth.getScaAuthenticationData());
                       consentAuthorization.setPassword(conAuth.getPassword());
                       return consentAuthorization;
                   })
                   .orElse(null);
    }

    public AisConsentAuthorizationRequest mapToAisConsentAuthorization(SpiScaStatus scaStatus) {
        return Optional.ofNullable(scaStatus)
                   .map(st -> {
                       AisConsentAuthorizationRequest consentAuthorization = new AisConsentAuthorizationRequest();
                       consentAuthorization.setScaStatus(CmsScaStatus.valueOf(st.name()));
                       return consentAuthorization;
                   })
                   .orElse(null);
    }

    public SpiAccountConsentAuthorization mapToSpiAccountConsentAuthorization(AisConsentAuthorizationResponse response) {
        return Optional.ofNullable(response)
                   .map(resp -> {
                       SpiAccountConsentAuthorization consentAuthorization = new SpiAccountConsentAuthorization();

                       consentAuthorization.setId(resp.getAuthorizationId());
                       consentAuthorization.setConsentId(resp.getConsentId());
                       consentAuthorization.setPsuId(resp.getPsuId());
                       consentAuthorization.setScaStatus(SpiScaStatus.valueOf(resp.getScaStatus().name()));
                       consentAuthorization.setAuthenticationMethodId(resp.getAuthenticationMethodId());
                       consentAuthorization.setScaAuthenticationData(resp.getScaAuthenticationData());
                       consentAuthorization.setPassword(resp.getPassword());
                       return consentAuthorization;
                   })
                   .orElse(null);
    }

    public AisConsentAuthorizationRequest mapToAisConsentAuthorizationRequest(SpiUpdateConsentPsuDataReq updatePsuData) {
        return Optional.ofNullable(updatePsuData)
                   .map(data -> {
                       AisConsentAuthorizationRequest consentAuthorization = new AisConsentAuthorizationRequest();
                       consentAuthorization.setPsuId(data.getPsuId());
                       consentAuthorization.setScaStatus(CmsScaStatus.valueOf(data.getScaStatus().name()));
                       consentAuthorization.setAuthenticationMethodId(data.getAuthenticationMethodId());
                       consentAuthorization.setPassword(data.getPassword());
                       consentAuthorization.setScaAuthenticationData(data.getScaAuthenticationData());

                       return consentAuthorization;
                   })
                   .orElse(null);
    }

    private Xs2aAccountAccess mapToAccountAccess(SpiAccountAccess access) {
        return Optional.ofNullable(access)
                   .map(aa ->
                            new Xs2aAccountAccess(
                                accountMapper.mapToAccountReferences(aa.getAccounts()),
                                accountMapper.mapToAccountReferences(aa.getBalances()),
                                accountMapper.mapToAccountReferences(aa.getTransactions()),
                                mapToAccountAccessType(aa.getAvailableAccounts()),
                                mapToAccountAccessType(aa.getAllPsd2()))
                   )
                   .orElse(null);
    }

    private Xs2aAccountAccessType mapToAccountAccessType(SpiAccountAccessType accessType) {
        return Optional.ofNullable(accessType)
                   .map(at -> Xs2aAccountAccessType.valueOf(at.name()))
                   .orElse(null);
    }

    private AisAccountAccessInfo mapToAisAccountAccessInfo(Xs2aAccountAccess access) {
        AisAccountAccessInfo accessInfo = new AisAccountAccessInfo();
        accessInfo.setAccounts(Optional.ofNullable(access.getAccounts())
                                   .map(this::mapToListAccountInfo)
                                   .orElseGet(Collections::emptyList));

        accessInfo.setBalances(Optional.ofNullable(access.getBalances())
                                   .map(this::mapToListAccountInfo)
                                   .orElseGet(Collections::emptyList));

        accessInfo.setTransactions(Optional.ofNullable(access.getTransactions())
                                       .map(this::mapToListAccountInfo)
                                       .orElseGet(Collections::emptyList));

        accessInfo.setAvailableAccounts(Optional.ofNullable(access.getAvailableAccounts())
                                            .map(accessType -> AccountAccessType.valueOf(accessType.name()))
                                            .orElse(null));
        accessInfo.setAllPsd2(Optional.ofNullable(access.getAllPsd2())
                                  .map(accessType -> AccountAccessType.valueOf(accessType.name()))
                                  .orElse(null));

        return accessInfo;
    }

    private List<AccountInfo> mapToListAccountInfo(List<AccountReference> refs) {
        return refs.stream()
                   .map(this::mapToAccountInfo)
                   .collect(Collectors.toList());
    }

    private AccountInfo mapToAccountInfo(AccountReference ref) {
        AccountInfo info = new AccountInfo();
        info.setIban(ref.getIban());
        info.setCurrency(Optional.ofNullable(ref.getCurrency())
                             .map(Currency::getCurrencyCode)
                             .orElse(null));
        return info;
    }
}
