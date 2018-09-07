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

package de.adorsys.aspsp.xs2a.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.adorsys.aspsp.xs2a.component.JsonConverter;
import de.adorsys.aspsp.xs2a.domain.CashAccountType;
import de.adorsys.aspsp.xs2a.domain.Transactions;
import de.adorsys.aspsp.xs2a.domain.account.Xs2aAccountDetails;
import de.adorsys.aspsp.xs2a.domain.account.Xs2aAccountReport;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiTransaction;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AccountMapperTest {
    private static final String SPI_ACCOUNT_DETAILS_JSON_PATH = "/json/MapSpiAccountDetailsToXs2aAccountDetailsTest.json";
    private static final String SPI_TRANSACTION_JSON_PATH = "/json/AccountReportDataTest.json";
    private static final Charset UTF_8 = Charset.forName("utf-8");

    @InjectMocks
    private AccountMapper accountMapper;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private JsonConverter jsonConverter = new JsonConverter(objectMapper);

    @Test
    public void mapSpiAccountDetailsToXs2aAccountDetails() throws IOException {
        //Given:
        String spiAccountDetailsJson = IOUtils.resourceToString(SPI_ACCOUNT_DETAILS_JSON_PATH, UTF_8);
        SpiAccountDetails donorAccountDetails = jsonConverter.toObject(spiAccountDetailsJson, SpiAccountDetails.class).get();

        //When:
        assertNotNull(donorAccountDetails);
        Xs2aAccountDetails actualAccountDetails = accountMapper.mapToAccountDetails(donorAccountDetails);

        //Then:
        assertThat(actualAccountDetails.getId()).isEqualTo("3dc3d5b3-7023-4848-9853-f5400a64e80f");
        assertThat(actualAccountDetails.getIban()).isEqualTo("DE2310010010123456789");
        assertThat(actualAccountDetails.getBban()).isEqualTo("DE2310010010123452343");
        assertThat(actualAccountDetails.getProduct()).isEqualTo("Girokonto");
        assertThat(actualAccountDetails.getName()).isEqualTo("Main Account");
        assertThat(actualAccountDetails.getCashAccountType()).isEqualTo(CashAccountType.CACC);
        assertThat(actualAccountDetails.getBic()).isEqualTo("EDEKDEHHXXX");
    }

    @Test
    public void mapAccountReport() throws IOException {
        //Given:
        String spiTransactionJson = IOUtils.resourceToString(SPI_TRANSACTION_JSON_PATH, UTF_8);
        SpiTransaction donorSpiTransaction = jsonConverter.toObject(spiTransactionJson, SpiTransaction.class).get();
        List<SpiTransaction> donorSpiTransactions = new ArrayList<>();
        donorSpiTransactions.add(donorSpiTransaction);
        SpiTransaction[] expectedBooked = donorSpiTransactions.stream()
                                              .filter(transaction -> transaction.getBookingDate() != null)
                                              .toArray(SpiTransaction[]::new);

        //When:
        assertNotNull(donorSpiTransaction);
        Optional<Xs2aAccountReport> aAR = accountMapper.mapToAccountReport(donorSpiTransactions);
        Xs2aAccountReport actualAccountReport;
        actualAccountReport = aAR.orElseGet(() -> new Xs2aAccountReport(new Transactions[]{}, new Transactions[]{}));


        //Then:
        assertThat(actualAccountReport.getBooked()[0].getTransactionId())
            .isEqualTo(expectedBooked[0].getTransactionId());
        assertThat(actualAccountReport.getBooked()[0].getBookingDate()).isEqualTo(expectedBooked[0].getBookingDate());
        assertThat(actualAccountReport.getBooked()[0].getCreditorId()).isEqualTo(expectedBooked[0].getCreditorId());
        assertThat(actualAccountReport.getBooked()[0].getCreditorName()).isEqualTo(expectedBooked[0].getCreditorName());
        assertThat(actualAccountReport.getBooked()[0].getDebtorName()).isEqualTo(expectedBooked[0].getDebtorName());
        assertThat(actualAccountReport.getBooked()[0].getEndToEndId()).isEqualTo(expectedBooked[0].getEndToEndId());
        assertThat(actualAccountReport.getBooked()[0].getMandateId()).isEqualTo(expectedBooked[0].getMandateId());
        assertThat(actualAccountReport.getBooked()[0].getRemittanceInformationStructured()).isEqualTo(expectedBooked[0].getRemittanceInformationStructured());
        assertThat(actualAccountReport.getBooked()[0].getRemittanceInformationUnstructured()).isEqualTo(expectedBooked[0].getRemittanceInformationUnstructured());
        assertThat(actualAccountReport.getBooked()[0].getUltimateCreditor()).isEqualTo(expectedBooked[0].getUltimateCreditor());
        assertThat(actualAccountReport.getBooked()[0].getValueDate()).isEqualTo(expectedBooked[0].getValueDate());
        assertThat(actualAccountReport.getBooked()[0].getAmount().getAmount()).isEqualTo(expectedBooked[0].getSpiAmount()
                                                                                              .getAmount().toString());
        assertThat(actualAccountReport.getBooked()[0].getAmount().getCurrency()).isEqualTo(expectedBooked[0].getSpiAmount()
                                                                                               .getCurrency());
        assertThat(actualAccountReport.getBooked()[0].getBankTransactionCodeCode()
                       .getCode()).isEqualTo(expectedBooked[0].getBankTransactionCodeCode());
        assertThat(actualAccountReport.getBooked()[0].getPurposeCode().getCode()).isEqualTo(expectedBooked[0].getPurposeCode());
    }
}
