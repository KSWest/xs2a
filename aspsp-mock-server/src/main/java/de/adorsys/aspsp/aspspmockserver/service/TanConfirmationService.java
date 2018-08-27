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

package de.adorsys.aspsp.aspspmockserver.service;

import de.adorsys.aspsp.aspspmockserver.repository.PsuRepository;
import de.adorsys.aspsp.aspspmockserver.repository.TanRepository;
import de.adorsys.aspsp.xs2a.spi.domain.psu.Tan;
import de.adorsys.aspsp.xs2a.spi.domain.psu.TanStatus;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.adorsys.aspsp.xs2a.spi.domain.psu.TanStatus.UNUSED;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class TanConfirmationService {
    private final TanRepository tanRepository;
    private final PsuRepository psuRepository;
    private final static String EMAIL_TEMPLATE_PATH = "email/email-template.html";
    private final JavaMailSender emailSender;
    private final Configuration fmConfiguration;
    private final AccountService accountService;

    /**
     * Generates new Tan and sends it to psu's email for payment confirmation
     *
     * @param iban Iban of Psu in order to get correct Psu and than get psu's email
     * @return true if psu was found and new Tan was sent successfully, otherwise return false
     */
    public boolean generateAndSendTanForPsuByIban(String iban) {
        return accountService.getPsuIdByIban(iban)
                   .map(this::generateAndSendTanForPsu)
                   .orElse(false);
    }

    private boolean generateAndSendTanForPsu(String psuId) {
        return Optional.ofNullable(psuRepository.findOne(psuId))
                   .map(psu -> createAndSendTan(psu.getId(), psu.getEmail()))
                   .orElse(false);
    }

    /**
     * Gets new Tan and sends it to psu's email for payment confirmation
     *
     * @param iban      Iban of Psu in order to get correct Psu
     * @param tanNumber TAN
     * @return true if Tan has status UNUSED, otherwise return false
     */
    public boolean isTanNumberValidByIban(String iban, String tanNumber) {
        return accountService.getPsuIdByIban(iban)
                   .map(psuId -> isPsuTanNumberValid(psuId, tanNumber))
                   .orElse(false);
    }

    /**
     * Gets number of attempts
     *
     * @param iban Iban of Psu in order to get correct Psu
     * @return number of attempts to validate tan
     */
    public int getTanNumberOfAttemptsByIban(String iban) {
        return accountService.getPsuIdByIban(iban)
                   .flatMap(psuId -> tanRepository.findByPsuId(psuId)
                                         .map(Tan::getNumberOfAttempts))
                   .orElse(0);
    }

    private boolean isPsuTanNumberValid(String psuId, String tanNumber) {
        return tanRepository.findByPsuIdAndTanStatus(psuId, UNUSED).stream()
                   .findFirst()
                   .map(t -> validateTanAndUpdateTanStatus(t, tanNumber))
                   .orElse(false);
    }

    private boolean createAndSendTan(String psuId, String email) {
        changeOldTansToInvalid(psuId);
        Tan tan = new Tan(psuId, generateTanNumber());
        tan = tanRepository.save(tan);

        return sendTanNumberOnEmail(email, tan.getTanNumber());
    }

    private void changeOldTansToInvalid(String psuId) {
        List<Tan> tans = tanRepository.findByPsuIdAndTanStatus(psuId, UNUSED);
        if (isNotEmpty(tans)) {
            for (Tan oldTan : tans) {
                oldTan.setTanStatus(TanStatus.INVALID);
            }
            tanRepository.save(tans);
        }
    }

    private boolean validateTanAndUpdateTanStatus(Tan originalTan, String givenTanNumber) {
        boolean isTanValid = originalTan.getTanNumber().equals(givenTanNumber);
        if (isTanValid) {
            originalTan.setTanStatus(TanStatus.VALID);
        } else {
            if (originalTan.getNumberOfAttempts() == 2) {
                originalTan.setTanStatus(TanStatus.INVALID);
            }
            originalTan.incrementNumberOfAttempts();
        }
        tanRepository.save(originalTan);
        return isTanValid;
    }

    private String generateTanNumber() {
        return RandomStringUtils.random(6, true, true);
    }

    private boolean sendTanNumberOnEmail(String email, String tanNumber) {
        if (emailSender == null) {
            log.warn("Email properties has not been set");
            return false;
        }

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        try {
            MimeMessageHelper mail = new MimeMessageHelper(mimeMessage, true);
            mail.setSubject("Your TAN for payment confirmation");
            mail.setFrom(email);
            mail.setTo(email);
            mail.setText(getEmailContentFromTemplate(tanNumber), true);

            emailSender.send(mail.getMimeMessage());
            return true;
        } catch (MessagingException e) {
            log.warn("Problem with creating or sanding email: {}", e);
            return false;
        }
    }

    private String getEmailContentFromTemplate(String tanNumber) {
        StringBuilder content = new StringBuilder();
        try {
            Template emailTemplate = fmConfiguration.getTemplate(EMAIL_TEMPLATE_PATH);
            content.append(FreeMarkerTemplateUtils.processTemplateIntoString(emailTemplate, Collections.singletonMap("tan", tanNumber)));
        } catch (Exception e) {
            log.warn("Problem with reading email template : {}", e);
            return "Your TAN number is " + tanNumber;
        }
        return content.toString();
    }
}
