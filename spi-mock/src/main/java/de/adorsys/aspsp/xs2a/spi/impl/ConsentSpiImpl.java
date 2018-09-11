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

package de.adorsys.aspsp.xs2a.spi.impl;

import de.adorsys.aspsp.xs2a.consent.api.ActionStatus;
import de.adorsys.aspsp.xs2a.consent.api.AisConsentStatusResponse;
import de.adorsys.aspsp.xs2a.consent.api.CmsScaStatus;
import de.adorsys.aspsp.xs2a.consent.api.ConsentActionRequest;
import de.adorsys.aspsp.xs2a.consent.api.ais.*;
import de.adorsys.aspsp.xs2a.consent.api.pis.authorisation.GetPisConsentAuthorizationResponse;
import de.adorsys.aspsp.xs2a.consent.api.pis.authorisation.UpdatePisConsentPsuDataRequest;
import de.adorsys.aspsp.xs2a.consent.api.pis.authorisation.UpdatePisConsentPsuDataResponse;
import de.adorsys.aspsp.xs2a.consent.api.pis.proto.CreatePisConsentResponse;
import de.adorsys.aspsp.xs2a.consent.api.pis.proto.PisConsentRequest;
import de.adorsys.aspsp.xs2a.spi.config.rest.consent.SpiAisConsentRemoteUrls;
import de.adorsys.aspsp.xs2a.spi.config.rest.consent.SpiPisConsentRemoteUrls;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountConsentAuthorization;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.aspsp.xs2a.spi.domain.consent.*;
import de.adorsys.aspsp.xs2a.spi.domain.psu.SpiScaMethod;
import de.adorsys.aspsp.xs2a.spi.impl.mapper.SpiAisConsentMapper;
import de.adorsys.aspsp.xs2a.spi.impl.mapper.SpiPisConsentMapper;
import de.adorsys.aspsp.xs2a.spi.impl.service.AspspService;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.aspsp.xs2a.spi.service.ConsentSpi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConsentSpiImpl implements ConsentSpi {

    @Qualifier("spiConsentRestTemplate")
    private final RestTemplate consentRestTemplate;
    private final SpiAisConsentRemoteUrls remoteAisConsentUrls;
    private final SpiPisConsentRemoteUrls remotePisConsentUrls;
    private final SpiAisConsentMapper aisConsentMapper;
    private final SpiPisConsentMapper pisConsentMapper;
    private final AccountSpi accountSpi;
    private final AspspService aspspService;

    /**
     * For detailed description see {@link ConsentSpi#createConsent(SpiCreateAisConsentRequest)}
     */
    @Override
    public String createConsent(SpiCreateAisConsentRequest spiCreateAisConsentRequest) {
        if (isDirectAccessRequest(spiCreateAisConsentRequest) && isInvalidSpiAccountAccessRequest(spiCreateAisConsentRequest.getAccess())) {
            return null;
        }

        CreateAisConsentRequest createAisConsentRequest = aisConsentMapper.mapToCmsCreateAisConsentRequest(spiCreateAisConsentRequest);
        CreateAisConsentResponse createAisConsentResponse = consentRestTemplate.postForEntity(remoteAisConsentUrls.createAisConsent(), createAisConsentRequest, CreateAisConsentResponse.class).getBody();

        return Optional.ofNullable(createAisConsentResponse)
                   .map(CreateAisConsentResponse::getConsentId)
                   .orElse(null);
    }

    /**
     * For detailed description see {@link ConsentSpi#getAccountConsentById(String)}
     */
    @Override
    public SpiAccountConsent getAccountConsentById(String consentId) {
        return consentRestTemplate.getForEntity(remoteAisConsentUrls.getAisConsentById(), SpiAccountConsent.class, consentId).getBody();
    }

    /**
     * For detailed description see {@link ConsentSpi#getAccountConsentStatusById(String)}
     */
    @Override
    public SpiConsentStatus getAccountConsentStatusById(String consentId) {
        AisConsentStatusResponse response = consentRestTemplate.getForEntity(remoteAisConsentUrls.getAisConsentStatusById(), AisConsentStatusResponse.class, consentId).getBody();
        return aisConsentMapper.mapToSpiConsentStatus(response.getConsentStatus())
                   .orElse(null);
    }

    /**
     * For detailed description see {@link ConsentSpi#revokeConsent(String)}
     */
    @Override
    public void revokeConsent(String consentId) {
        consentRestTemplate.put(remoteAisConsentUrls.updateAisConsentStatus(), null, consentId, SpiConsentStatus.REVOKED_BY_PSU);
    }

    /**
     * For detailed description see {@link ConsentSpi#consentActionLog(String, String, ActionStatus)}
     */
    @Override
    public void consentActionLog(String tppId, String consentId, ActionStatus actionStatus) {
        consentRestTemplate.postForEntity(remoteAisConsentUrls.consentActionLog(), new ConsentActionRequest(tppId, consentId, actionStatus), Void.class);
    }


    /**
     * Sends a POST request to CMS to store created consent authorization
     *
     * @param paymentId String representation of identifier of stored consent
     * @return long representation of identifier of stored consent authorization
     */
    @Override
    public SpiCreatePisConsentAuthorizationResponse createPisConsentAuthorization(String paymentId) {
        return consentRestTemplate.postForEntity(remotePisConsentUrls.createPisConsentAuthorization(),
            null, SpiCreatePisConsentAuthorizationResponse.class, paymentId)
                   .getBody();
    }

    /**
     * Sends a POST request to CMS to store created consent authorization
     *
     * @param consentId String representation of identifier of stored consent
     * @return long representation of identifier of stored consent authorization
     */
    @Override
    public Optional<String> createAisConsentAuthorization(String consentId, SpiScaStatus scaStatus) {
        AisConsentAuthorizationRequest request = aisConsentMapper.mapToAisConsentAuthorization(scaStatus);

        CreateAisConsentAuthorizationResponse response = consentRestTemplate.postForEntity(remoteAisConsentUrls.createAisConsentAuthorization(),
            request, CreateAisConsentAuthorizationResponse.class, consentId).getBody();

        return Optional.ofNullable(response)
                   .map(CreateAisConsentAuthorizationResponse::getAuthorizationId);
    }

    /**
     * Requests CMS to retrieve AIS consent authorization by its identifier
     *
     * @param authorizationId String representation of identifier of stored consent authorization
     * @return Response containing AIS Consent Authorization
     */
    @Override
    public SpiAccountConsentAuthorization getAccountConsentAuthorizationById(String authorizationId, String consentId) {
        AisConsentAuthorizationResponse resp = consentRestTemplate.getForEntity(remoteAisConsentUrls.getAisConsentAuthorizationById(), AisConsentAuthorizationResponse.class, consentId, authorizationId).getBody();

        return aisConsentMapper.mapToSpiAccountConsentAuthorization(resp);
    }

    /**
     * Sends a PUT request to CMS to update created AIS consent authorization
     *
     * @param updatePsuData Consent psu data
     */
    @Override
    public void updateConsentAuthorization(SpiUpdateConsentPsuDataReq updatePsuData) {
        final String consentId = updatePsuData.getConsentId();
        final String authorizationId = updatePsuData.getAuthorizationId();
        final AisConsentAuthorizationRequest request = aisConsentMapper.mapToAisConsentAuthorizationRequest(updatePsuData);

        consentRestTemplate.put(remoteAisConsentUrls.updateAisConsentAuthorization(), request, AisConsentAuthorizationResponse.class, consentId, authorizationId);
    }

    /**
     * For detailed description see {@link ConsentSpi#createPisConsentForSinglePaymentAndGetId(SpiPisConsentRequest)}
     */
    @Override
    public CreatePisConsentResponse createPisConsentForSinglePaymentAndGetId(SpiPisConsentRequest spiPisConsentRequest) {
        PisConsentRequest cmsPisConsentRequest = pisConsentMapper.mapToCmsPisConsentRequestForSinglePayment(spiPisConsentRequest);
        CreatePisConsentResponse createPisConsentResponse = consentRestTemplate.postForEntity(remotePisConsentUrls.createPisConsent(), cmsPisConsentRequest, CreatePisConsentResponse.class).getBody();

        return Optional.ofNullable(createPisConsentResponse)
                   .orElse(null);
    }

    /**
     * For detailed description see {@link ConsentSpi#createPisConsentForBulkPaymentAndGetId(SpiPisConsentRequest)}
     */
    @Override
    public CreatePisConsentResponse createPisConsentForBulkPaymentAndGetId(SpiPisConsentRequest spiPisConsentRequest) {
        PisConsentRequest pisConsentRequest = pisConsentMapper.mapToCmsPisConsentRequestForBulkPayment(spiPisConsentRequest);
        CreatePisConsentResponse createPisConsentResponse = consentRestTemplate.postForEntity(remotePisConsentUrls.createPisConsent(), pisConsentRequest, CreatePisConsentResponse.class).getBody();

        return Optional.ofNullable(createPisConsentResponse)
                   .orElse(null);
    }

    /**
     * For detailed description see {@link ConsentSpi#createPisConsentForPeriodicPaymentAndGetId(SpiPisConsentRequest)}
     */
    @Override
    public CreatePisConsentResponse createPisConsentForPeriodicPaymentAndGetId(SpiPisConsentRequest spiPisConsentRequest) {
        PisConsentRequest pisConsentRequest = pisConsentMapper.mapToCmsPisConsentRequestForPeriodicPayment(spiPisConsentRequest);
        CreatePisConsentResponse createPisConsentResponse = consentRestTemplate.postForEntity(remotePisConsentUrls.createPisConsent(), pisConsentRequest, CreatePisConsentResponse.class).getBody();

        return Optional.ofNullable(createPisConsentResponse)
                   .orElse(null);
    }


    @Override
    public UpdatePisConsentPsuDataResponse updatePisConsentAuthorization(UpdatePisConsentPsuDataRequest request) {
        GetPisConsentAuthorizationResponse authorizationResponse = consentRestTemplate.exchange(remotePisConsentUrls.getPisConsentAuthorizationById(), HttpMethod.GET, new HttpEntity<>(request), GetPisConsentAuthorizationResponse.class, request.getAuthorizationId())
                                                                       .getBody();
        if (CmsScaStatus.STARTED == authorizationResponse.getScaStatus()) {
            List<SpiScaMethod> spiScaMethods = aspspService.readAvailableScaMethod(request.getPsuId(), request.getPassword());

            if (CollectionUtils.isEmpty(spiScaMethods)) {
                String executionPaymentId = aspspService.createPayment(authorizationResponse.getPaymentType(), authorizationResponse.getPayments());
                request.setExecutionPaymentId(executionPaymentId);
                request.setScaStatus(CmsScaStatus.FINALISED);

                UpdatePisConsentPsuDataResponse updatePisConsentPsuDataResponse = consentRestTemplate.exchange(remotePisConsentUrls.updatePisConsentAuthorization(), HttpMethod.PUT, new HttpEntity<>(request),
                    UpdatePisConsentPsuDataResponse.class, request.getAuthorizationId()).getBody();
                return new UpdatePisConsentPsuDataResponse(updatePisConsentPsuDataResponse.getScaStatus());
            } else if (hasSingleValue(spiScaMethods)) {
                aspspService.generateConfirmationCode();
                request.setScaStatus(CmsScaStatus.SCAMETHODSELECTED);

                UpdatePisConsentPsuDataResponse updatePisConsentPsuDataResponse = consentRestTemplate.exchange(remotePisConsentUrls.updatePisConsentAuthorization(), HttpMethod.PUT, new HttpEntity<>(request),
                    UpdatePisConsentPsuDataResponse.class, request.getAuthorizationId()).getBody();
                return new UpdatePisConsentPsuDataResponse(updatePisConsentPsuDataResponse.getScaStatus());
            }
        }
        return new UpdatePisConsentPsuDataResponse();
    }

    private boolean hasSingleValue(List<SpiScaMethod> spiScaMethods) {
        return spiScaMethods.size() == 1;
    }

    private boolean isDirectAccessRequest(SpiCreateAisConsentRequest spiCreateAisConsentRequest) {
        SpiAccountAccess spiAccountAccess = spiCreateAisConsentRequest.getAccess();
        return CollectionUtils.isNotEmpty(spiAccountAccess.getBalances())
                   || CollectionUtils.isNotEmpty(spiAccountAccess.getAccounts())
                   || CollectionUtils.isNotEmpty(spiAccountAccess.getTransactions());
    }

    private boolean isInvalidSpiAccountAccessRequest(SpiAccountAccess requestedAccess) {
        Set<String> ibansFromAccess = getIbansFromAccess(requestedAccess);
        List<SpiAccountDetails> accountDetailsList = accountSpi.readAccountDetailsByIbans(
            ibansFromAccess,
            new AspspConsentData("zzzzzzzzzzzzzz".getBytes())).getPayload();

        return ibansFromAccess.stream()
                   .map(acc -> filter(acc, accountDetailsList))
                   .anyMatch(a -> !a);
    }

    private boolean filter(String iban, List<SpiAccountDetails> accountDetailsList) {
        return accountDetailsList.stream()
                   .map(acc -> acc.getIban().equals(iban))
                   .findAny()
                   .orElse(false);
    }

    private Set<String> getIbansFromAccess(SpiAccountAccess access) {
        return Stream.of(
            getIbansFromAccountReference(access.getAccounts()),
            getIbansFromAccountReference(access.getBalances()),
            getIbansFromAccountReference(access.getTransactions())
        )
                   .flatMap(Collection::stream)
                   .collect(Collectors.toSet());
    }

    private Set<String> getIbansFromAccountReference(List<SpiAccountReference> references) {
        return Optional.ofNullable(references)
                   .map(list -> list.stream()
                                    .map(SpiAccountReference::getIban)
                                    .collect(Collectors.toSet()))
                   .orElseGet(Collections::emptySet);
    }
}
