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

package de.adorsys.aspsp.xs2a.service.keycloak;

import de.adorsys.aspsp.xs2a.config.KeycloakConfigProperties;
import de.adorsys.aspsp.xs2a.config.rest.BearerToken;
import de.adorsys.aspsp.xs2a.spi.domain.constant.AuthorizationConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Optional;

import static de.adorsys.aspsp.xs2a.spi.domain.constant.AuthorizationConstant.BEARER_TOKEN_PREFIX;

@Service
public class KeycloakInvokerService {
    @Autowired
    private BearerToken bearerToken;
    @Autowired
    private KeycloakConfigProperties keycloakConfig;
    @Autowired
    @Qualifier("keycloakRestTemplate")
    private RestTemplate keycloakRestTemplate;

    @Value("${keycloak-username}")
    private String keycloakUsername;
    @Value("${keycloak-password}")
    private String keycloakPassword;

    // TODO move the user authorisation logic to AspspConsentData https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/297
    public String obtainAccessToken(String userName, String password) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", userName);
        params.add("password", password);
        String token = Optional.ofNullable(doObtainAccessToken(params))
                       .map(t -> AuthorizationConstant.AUTHORIZATION_HEADER + ": " + BEARER_TOKEN_PREFIX + t)
                       .orElse(null);
        bearerToken.setToken(token);
        return token;
    }

    public String obtainAccessToken() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", keycloakUsername);
        params.add("password", keycloakPassword);

        return Optional.ofNullable(doObtainAccessToken(params))
                   .map(token -> AuthorizationConstant.AUTHORIZATION_HEADER + ": " + BEARER_TOKEN_PREFIX + token)
                   .orElse(null);
    }

    private String doObtainAccessToken(MultiValueMap<String, String> params) {
        params.add("grant_type", "password");
        params.add("client_id", keycloakConfig.getResource());
        params.add("client_secret", keycloakConfig.getCredentials().getSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<HashMap<String, String>> response = keycloakRestTemplate.exchange(keycloakConfig.getRootPath() + "/protocol/openid-connect/token", HttpMethod.POST, new HttpEntity<>(params, headers),
            new ParameterizedTypeReference<HashMap<String, String>>() {
            });

        return Optional.ofNullable(response.getBody())
                   .map(body -> body.get(AuthorizationConstant.ACCESS_TOKEN))
                   .orElse(null);
    }
}
