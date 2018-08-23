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
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import de.adorsys.aspsp.xs2a.integtest.model.TestData;
import de.adorsys.aspsp.xs2a.integtest.util.Context;
import de.adorsys.psd2.model.BulkPaymentInitiationSctJson;
import de.adorsys.psd2.model.PaymentInitationRequestResponse201;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@FeatureFileSteps
public class BulkPaymentSteps {
    @Autowired
    @Qualifier("xs2a")
    private RestTemplate restTemplate;

    @Autowired
    private Context<BulkPaymentInitiationSctJson, List<PaymentInitationRequestResponse201>> context;

    @Autowired
    private ObjectMapper mapper;

    @Given("^PSU wants to initiate multiple payments (.*) using the payment product (.*)$")
    public void loadTestDataBulkPayment(String dataFileName, String paymentProduct) throws IOException {
        context.setPaymentProduct(paymentProduct);

        TestData<BulkPaymentInitiationSctJson, List<PaymentInitationRequestResponse201>> data = mapper.readValue(resourceToString("/data-input/pis/bulk/" + dataFileName, UTF_8), new TypeReference<TestData<BulkPaymentInitiationSctJson, List<PaymentInitationRequestResponse201>>>() {
        });

        context.setTestData(data);
    }

    //TODO Uncomment after ISO DateTime format rework
    /*@When("^PSU sends the bulk payment initiating request$")
    public void sendBulkPaymentInitiatingRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(context.getTestData().getRequest().getHeader());
        headers.add("Authorization", "Bearer " + context.getAccessToken());
        headers.add("Content-Type", "application/json");

        List<BulkPaymentInitiationSctJson> paymentsList = context.getTestData().getRequest().getBody();

        ResponseEntity<List<PaymentInitationRequestResponse201>> response = restTemplate.exchange(
            context.getBaseUrl() + "/bulk-payments/" + context.getPaymentProduct(),
            HttpMethod.POST, new HttpEntity<>(paymentsList, headers), new ParameterizedTypeReference<List<PaymentInitationRequestResponse201>>() {
            });

        context.setActualResponse(response);
    }*/

    @Then("^a successful response code and the appropriate bulk payment response data$")
    public void checkResponseCodeBulkPayment() {
        ResponseEntity<List<PaymentInitationRequestResponse201>> actualResponse = context.getActualResponse();
        List<PaymentInitationRequestResponse201> givenResponseBody = context.getTestData().getResponse().getBody();

        assertThat(actualResponse.getStatusCode(), equalTo(context.getTestData().getResponse().getHttpStatus()));

        assertThat(actualResponse.getBody().get(0).getTransactionStatus().name(), equalTo(givenResponseBody.get(0).getTransactionStatus()));
        assertThat(actualResponse.getStatusCode(), notNullValue());

        assertThat(actualResponse.getBody().get(1).getTransactionStatus().name(), equalTo(givenResponseBody.get(1).getTransactionStatus()));
    }

    @And("^a redirect URL for every payment of the Bulk payment is delivered to the PSU$")
    public void checkRedirectUrlBulkPayment() {
        ResponseEntity<List<PaymentInitationRequestResponse201>> actualResponse = context.getActualResponse();

        assertThat(actualResponse.getBody().get(0).getLinks().get("scaRedirect"), notNullValue());
        assertThat(actualResponse.getBody().get(1).getLinks().get("scaRedirect"), notNullValue());
    }
}

