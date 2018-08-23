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
import cucumber.api.java.en.When;
import de.adorsys.aspsp.xs2a.integtest.entities.ITMessageError;
import de.adorsys.aspsp.xs2a.integtest.model.TestData;
import de.adorsys.aspsp.xs2a.integtest.util.Context;
import de.adorsys.psd2.model.PaymentInitationRequestResponse201;
import de.adorsys.psd2.model.PaymentInitiationSctJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@FeatureFileSteps
public class SinglePaymentSteps {

    @Autowired
    @Qualifier("xs2a")
    private RestTemplate restTemplate;

    @Autowired
    private Context<PaymentInitiationSctJson, PaymentInitationRequestResponse201> context;

    @Autowired
    private ObjectMapper mapper;

    private String dataFileName;

    @Given("^PSU wants to initiate a single payment (.*) using the payment service (.*) and the payment product (.*)$")
    public void loadTestData(String dataFileName, String paymentService, String paymentProduct) throws IOException {
        context.setPaymentProduct(paymentProduct);
        context.setPaymentService(paymentService);
        this.dataFileName = dataFileName;

        TestData<PaymentInitiationSctJson, PaymentInitationRequestResponse201> data = mapper.readValue(resourceToString("/data-input/pis/single/" + dataFileName, UTF_8), new TypeReference<TestData<PaymentInitiationSctJson, PaymentInitationRequestResponse201>>() {
        });

        context.setTestData(data);
    }

    @When("^PSU sends the single payment initiating request$")
    public void sendPaymentInitiatingRequest() {
        HttpEntity<PaymentInitiationSctJson> entity = getSinglePaymentsHttpEntity();

        ResponseEntity<PaymentInitationRequestResponse201> response = restTemplate.exchange(
            context.getBaseUrl() + "/" + context.getPaymentService() + "/" + context.getPaymentProduct(),
            HttpMethod.POST,
            entity,
            PaymentInitationRequestResponse201.class);

        context.setActualResponse(response);
    }

    @Then("^a successful response code and the appropriate single payment response data$")
    public void checkResponseCode() {
        ResponseEntity<PaymentInitationRequestResponse201> actualResponse = context.getActualResponse();
        PaymentInitationRequestResponse201 givenResponseBody = context.getTestData().getResponse().getBody();

        assertThat(actualResponse.getStatusCode(), equalTo(context.getTestData().getResponse().getHttpStatus()));

        assertThat(actualResponse.getBody().getTransactionStatus().name(), equalTo(givenResponseBody.getTransactionStatus()));
        assertThat(actualResponse.getBody().getPaymentId(), notNullValue());
    }

    @And("^a redirect URL is delivered to the PSU$")
    public void checkRedirectUrl() {
        ResponseEntity<PaymentInitationRequestResponse201> actualResponse = context.getActualResponse();

        assertThat(actualResponse.getBody().getLinks().get("scaRedirect"), notNullValue());
    }

    @When("^PSU sends the single payment initiating request with error$")
    public void sendPaymentInitiatingRequestWithError() throws HttpClientErrorException, IOException {
        HttpEntity<PaymentInitiationSctJson> entity = getSinglePaymentsHttpEntity();

        try {
            restTemplate.exchange(
                context.getBaseUrl() + "/" + context.getPaymentService() + "/" + context.getPaymentProduct(),
                HttpMethod.POST,
                entity,
                PaymentInitiationSctJson.class);
        } catch (RestClientResponseException rex) {
            handleRequestError(rex);
        }
    }

    private void handleRequestError(RestClientResponseException exceptionObject) throws IOException {
        ResponseEntity<PaymentInitationRequestResponse201> actualResponse = new ResponseEntity<>(HttpStatus.valueOf(exceptionObject.getRawStatusCode()));
        context.setActualResponse(actualResponse);
        String responseBodyAsString = exceptionObject.getResponseBodyAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        ITMessageError messageError = objectMapper.readValue(responseBodyAsString, ITMessageError.class);
        context.setMessageError(messageError);
    }

    //TODO: Uncomment after finding solution for mapping TppMessages
//    @Then("^an error response code is displayed the appropriate error response$")
//    public void anErrorResponseCodeIsDisplayedTheAppropriateErrorResponse() {
//        ITMessageError givenErrorObject = context.getMessageError();
//        Map givenResponseBody = context.getTestData().getResponse().getBody();
//
//        HttpStatus httpStatus = context.getTestData().getResponse().getHttpStatus();
//        assertThat(context.getActualResponse().getStatusCode(), equalTo(httpStatus));
//
//        LinkedHashMap tppMessageContent = (LinkedHashMap) givenResponseBody.get("tppMessage");
//
//        // for cases when transactionStatus and tppMessage created after request
//        if (givenErrorObject.getTppMessage() != null) {
//            assertThat(givenErrorObject.getTransactionStatus().name(), equalTo(givenResponseBody.get("transactionStatus")));
//            assertThat(givenErrorObject.getTppMessage().getCategory().name(), equalTo(tppMessageContent.get("category")));
//            assertThat(givenErrorObject.getTppMessage().getCode().name(), equalTo(tppMessageContent.get("code")));
//        }
//    }

    private HttpEntity<PaymentInitiationSctJson> getSinglePaymentsHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(context.getTestData().getRequest().getHeader());
        headers.add("Authorization", "Bearer " + context.getAccessToken());
        headers.add("Content-Type", "application/json");

        return new HttpEntity<>(context.getTestData().getRequest().getBody(), headers);
    }
}
