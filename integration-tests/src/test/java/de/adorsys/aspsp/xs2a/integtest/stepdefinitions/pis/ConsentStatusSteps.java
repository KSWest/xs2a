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
import cucumber.api.java.en.When;
import de.adorsys.aspsp.xs2a.domain.consent.ConsentStatus;
import de.adorsys.aspsp.xs2a.domain.consent.ConsentStatusResponse;
import de.adorsys.aspsp.xs2a.integtest.model.TestData;
import de.adorsys.aspsp.xs2a.integtest.util.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;

public class ConsentStatusSteps {

    @Autowired
    @Qualifier("xs2a")
    private RestTemplate restTemplate;
    @Autowired
    private Context<ConsentStatus, HashMap, ConsentStatusResponse> context;
    @Autowired
    private ObjectMapper mapper;
    private String dataFileName;

    @Given("^AISP wants to get the status of a consent (.*) and the data (.*)$")
    public void loadTestData(String consentId, String dataFileName) throws IOException {
        this.dataFileName = dataFileName;
        //consentId = createConsent;
        TestData<ConsentStatus, HashMap> data = mapper.readValue(resourceToString("/data-input/ais/consent/" + dataFileName, UTF_8), new TypeReference<TestData<ConsentStatus, HashMap>>() {
        });
        context.setTestData(data);
    }

    @When("^AISP requests consent status$")
    public void sendConsentStatusRequest() throws HttpClientErrorException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(context.getTestData().getRequest().getHeader());
        headers.add("Authorization", "Bearer " + context.getAccessToken());
        headers.add("Content-Type", "application/json");
        HttpEntity<ConsentStatus> entity = new HttpEntity<>(context.getTestData().getRequest().getBody(), headers);

        ResponseEntity<ConsentStatusResponse> response = restTemplate.exchange(
            context.getBaseUrl() + "/consents",
            HttpMethod.GET,
            entity,
            ConsentStatusResponse.class);
        context.setActualResponse(response);
    }




}
