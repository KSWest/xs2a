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

import cucumber.api.java.en.Then;
import de.adorsys.aspsp.xs2a.integtest.util.Context;
import de.adorsys.psd2.model.TppMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


@FeatureFileSteps
public class GlobalErrorfulSteps {

    @Autowired
    private Context context;

    @Then("^an error response code and the appropriate error response are received")
    public void anErrorResponseCodeIsDisplayedTheAppropriateErrorResponse() {
        TppMessages actualTppMessages = context.getTppMessages();
        TppMessages givenTppMessages = (TppMessages) context.getTestData().getResponse().getBody();

        HttpStatus httpStatus = context.getTestData().getResponse().getHttpStatus();
        assertThat(context.getActualResponseStatus(), equalTo(httpStatus));

        assertThat(actualTppMessages, is(equalTo(givenTppMessages)));

        actualTppMessages.forEach ((msg) -> {
            assertThat(msg.getCategory().toString(), equalTo(givenTppMessages.get(msg.getCategory().ordinal()).getCategory().toString()));
            assertThat(msg.getCode().toString(), equalTo(givenTppMessages.get(msg.getCategory().ordinal()).getCode().toString()));
        });
    }
    // @After **** see GlobalSuccessfulSteps
}
