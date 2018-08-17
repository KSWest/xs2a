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

package de.adorsys.aspsp.xs2a.service.validator.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.aspsp.xs2a.service.validator.parameter.impl.AccountRequestParameter;
import de.adorsys.aspsp.xs2a.service.validator.parameter.impl.ErrorMessageParameterImpl;
import de.adorsys.aspsp.xs2a.service.validator.parameter.impl.NotMatchedParameterImpl;
import de.adorsys.aspsp.xs2a.web12.AccountController;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class ParametersFactory {

    private static final Map<Class, Class> controllerClassMap = new HashMap<>();

    static {
        controllerClassMap.put(AccountController.class, AccountRequestParameter.class);
    }

    private final ObjectMapper objectMapper;

    public RequestParameter getParameterImpl(Map<String, String> requestParametersMap, Class controllerClass) {
        Class<? extends RequestParameter> headerClass = controllerClassMap.get(controllerClass);

        if (headerClass == null) {
            return new NotMatchedParameterImpl();
        } else {
            try {
                return objectMapper.convertValue(requestParametersMap, headerClass);
            } catch (IllegalArgumentException exception) {
                log.error("Error request parameter conversion: " + exception.getMessage());
                return new ErrorMessageParameterImpl(exception.getMessage());
            }
        }
    }
}
