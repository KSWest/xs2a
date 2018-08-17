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

package de.adorsys.aspsp.xs2a.integtest.stepdefinitions.pis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.adorsys.aspsp.xs2a.domain.consent.CreateConsentReq;
import de.adorsys.aspsp.xs2a.domain.consent.CreateConsentResponse;
import de.adorsys.aspsp.xs2a.integtest.entities.ITMessageError;
import de.adorsys.aspsp.xs2a.integtest.model.TestData;
import de.adorsys.aspsp.xs2a.integtest.util.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import static org.apache.commons.io.IOUtils.resourceToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@FeatureFileSteps
public class ConsentRequestSteps {

    @Autowired
    @Qualifier("xs2a")
    private RestTemplate restTemplate;

    @Autowired
    private Context<CreateConsentReq, HashMap, CreateConsentResponse> context;

    @Autowired
    private ObjectMapper mapper;

    @Given("^PSU wants to create a consent (.*)$")
    public void loadTestData(String dataFileName) throws IOException {

        TestData<CreateConsentReq, HashMap> data = mapper.readValue(resourceToString("/data-input/ais/consent/" + dataFileName, UTF_8), new TypeReference<TestData<CreateConsentReq, HashMap>>() {
        });

        context.setTestData(data);

        LocalDate validUntil = context.getTestData().getRequest().getBody().getValidUntil();
        context.getTestData().getRequest().getBody().setValidUntil(validUntil.plusDays(7));
    }

    @When("^PSU sends the create consent request$")
    public void sendConsentRequest() throws HttpClientErrorException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(context.getTestData().getRequest().getHeader());
        headers.add("Authorization", "Bearer " + context.getAccessToken());
        headers.add("Content-Type", "application/json");
        HttpEntity<CreateConsentReq> entity = new HttpEntity<>(context.getTestData().getRequest().getBody(), headers);

        ResponseEntity<CreateConsentResponse> response = restTemplate.exchange(
            context.getBaseUrl() + "/consents",
            HttpMethod.POST,
            entity,
            CreateConsentResponse.class);

        context.setActualResponse(response);
    }

    @Then("^a successful response code and the appropriate consent response data is delivered to the PSU$")
    public void checkResponseCode() {
        ResponseEntity<CreateConsentResponse> actualResponse = context.getActualResponse();
        Map givenResponseBody = context.getTestData().getResponse().getBody();

        assertThat(actualResponse.getStatusCode(), equalTo(context.getTestData().getResponse().getHttpStatus()));
        assertThat(actualResponse.getBody().getConsentStatus(), equalTo(givenResponseBody.get("consentStatus")));
        assertThat(actualResponse.getBody().getConsentId(), notNullValue());
    }

    @When("^PSU sends the create consent request with error$")
    public void sendErrorfulConsentRequest() throws HttpClientErrorException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(context.getTestData().getRequest().getHeader());
        headers.add("Authorization", "Bearer " + context.getAccessToken());
        headers.add("Content-Type", "application/json");
        HttpEntity<CreateConsentReq> entity = new HttpEntity<>(context.getTestData().getRequest().getBody(), headers);
        try {
            restTemplate.exchange(
                context.getBaseUrl() + "/consents",
                HttpMethod.POST,
                entity,
                HashMap.class);
        } catch (RestClientResponseException rex) {
            handleRequestError(rex);
        }
    }

    private void handleRequestError(RestClientResponseException exceptionObject) throws IOException {
        ResponseEntity<CreateConsentResponse> actualResponse = new ResponseEntity<>(HttpStatus.valueOf(exceptionObject.getRawStatusCode()));
        context.setActualResponse(actualResponse);
        String responseBodyAsString = exceptionObject.getResponseBodyAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        ITMessageError messageError = objectMapper.readValue(responseBodyAsString, ITMessageError.class);
        context.setMessageError(messageError);
    }
}
