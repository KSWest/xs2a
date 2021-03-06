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

package de.adorsys.aspsp.cmsclient.cms.model.pis;

import de.adorsys.aspsp.cmsclient.cms.RestCmsRequestMethod;
import de.adorsys.aspsp.cmsclient.core.HttpMethod;
import de.adorsys.aspsp.xs2a.consent.api.pis.proto.CreatePisConsentResponse;
import de.adorsys.aspsp.xs2a.consent.api.pis.proto.PisConsentRequest;

public class CreatePaymentConsentMethod extends RestCmsRequestMethod<PisConsentRequest, CreatePisConsentResponse> {
    private static final String CREATE_PAYMENT_CONSENT_URI = "api/v1/pis/consent/";

    public CreatePaymentConsentMethod(final PisConsentRequest request) {
        super(request, HttpMethod.POST, CREATE_PAYMENT_CONSENT_URI);
    }
}
