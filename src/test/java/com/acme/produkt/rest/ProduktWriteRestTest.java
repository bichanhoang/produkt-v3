/*
 * Copyright (C) 2022 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.acme.produkt.rest;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import static com.acme.produkt.dev.DevConfig.DEV;
import static com.acme.produkt.rest.ProduktGetController.ID_PATTERN;
import static com.acme.produkt.rest.ProduktGetController.REST_PATH;
import static com.acme.produkt.rest.ProduktGetRestTest.HOST;
import static com.acme.produkt.rest.ProduktGetRestTest.SCHEMA;
import static java.math.BigDecimal.ONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Tag("integration")
@Tag("rest")
@Tag("rest_write")
@DisplayName("REST-Schnittstelle fuer Schreiben")
@ExtendWith(SoftAssertionsExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_19, max = JAVA_20)
@SuppressWarnings("WriteTag")
class ProduktWriteRestTest {
    private static final String ANGESTELLTER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String ID_VORHANDEN = "00000000-0000-0000-0000-000000000001";
    private static final String ID_UPDATE_PUT = "00000000-0000-0000-0000-000000000030";
    private static final String ID_UPDATE_PATCH = "00000000-0000-0000-0000-000000000040";
    private static final String NEUER_NAME = "Neuername-Rest";
    private static final String NEUES_ERSCHEINUNGSDATUM = "2022-01-31";
    private static final String CURRENCY_CODE = "EUR";
    private static final String NEUE_HOMEPAGE = "https://test.de";

    private static final String NEUER_NAME_INVALID = "?!$";
    private static final String NEUES_ERSCHEINUNGSSDATUM_INVALID = "3000-01-31";

    private static final String ID_PATH = "/{id}";

    private final WebClient client;

    @InjectSoftAssertions
    private SoftAssertions softly;

    ProduktWriteRestTest(@LocalServerPort final int port, final ApplicationContext ctx) {
        final var writeController = ctx.getBean(ProduktWriteController.class);
        assertThat(writeController).isNotNull();

        final var uriComponents = UriComponentsBuilder.newInstance()
            .scheme(SCHEMA)
            .host(HOST)
            .port(port)
            .path(REST_PATH)
            .build();
        final var baseUrl = uriComponents.toUriString();
        client = WebClient
            .builder()
            .baseUrl(baseUrl)
            .build();
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("REST-Schnittstelle fuer POST")
    class Erzeugen {
        @ParameterizedTest(name = "[{index}] Neuanlegen eines neuen Produktes: name={0}")
        @CsvSource(
            NEUER_NAME + "," + NEUES_ERSCHEINUNGSDATUM + "," + NEUE_HOMEPAGE + "," + CURRENCY_CODE + "," +
                ANGESTELLTER_ID

        )
        @DisplayName("Neuanlegen eines neuen Produktes")
        void create(final ArgumentsAccessor args) {
            // given
            final var umsatz = new UmsatzDTO(ONE, Currency.getInstance(args.getString(3)));
            final var produktDTO = new ProduktDTO(
                args.getString(0),
                args.get(1, LocalDate.class),
                args.get(2, URL.class),
                umsatz,
                UUID.fromString(args.get(4).toString())
            );

            // when
            final var response = client
                .post()
                .contentType(APPLICATION_JSON)
                .bodyValue(produktDTO)
                .exchangeToMono(Mono::just)
                .block();

            // then
            softly.assertThat(response)
                .isNotNull()
                .extracting(ClientResponse::statusCode)
                .isEqualTo(CREATED);
            final var location = response.headers().asHttpHeaders().getLocation();
            softly.assertThat(location)
                .isNotNull()
                .isInstanceOf(URI.class);
            softly.assertThat(location.toString()).matches(".*/" + ID_PATTERN + "$");
        }

        @ParameterizedTest(name = "[{index}] Neuanlegen mit ungueltigen Werten: name={0}, erscheinungsdatum={1}")
        @CsvSource(NEUER_NAME_INVALID + "," + NEUES_ERSCHEINUNGSSDATUM_INVALID)
        @DisplayName("Neuanlegen mit ungueltigen Werten")
        @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
        void createInvalid(final ArgumentsAccessor args) {
            // given
            final var produktDTO = new ProduktDTO(
                args.getString(0),
                args.get(1, LocalDate.class),
                null,
                null,
                null
            );

            final var violationKeys = List.of(
                "name",
                "erscheinungsdatum"
            );

            // when
            final var body = client
                .post()
                .contentType(APPLICATION_JSON)
                .bodyValue(produktDTO)
                .exchangeToMono(response -> {
                    assertThat(response)
                        .extracting(ClientResponse::statusCode)
                        .isEqualTo(UNPROCESSABLE_ENTITY);
                    return response.bodyToMono(ProblemDetail.class);
                })
                .block();

            // then
            assertThat(body).isNotNull();
            final var detail = body.getDetail();
            assertThat(detail).isNotNull();
            final var violations = Arrays.asList(detail.split(", ", -1));
            assertThat(violations).hasSameSizeAs(violationKeys);

            final var actualViolationKeys = violations
                .stream()
                // Keys vor ":" extrahieren. limit=-1 bedeutet, das Pattern beliebig oft anwenden
                .map(violation -> violation.split(": ", -1)[0])
                .toList();
            assertThat(actualViolationKeys)
                .hasSameSizeAs(violationKeys)
                .hasSameElementsAs(violationKeys);
        }
    }

    @Nested
    @DisplayName("REST-Schnittstelle fuer Aendern")
    class Aendern {
        @Nested
        @DisplayName("REST-Schnittstelle fuer Put")
        class AendernDurchPut {
            @ParameterizedTest(name = "[{index}] Aendern eines vorhandenen Produktes durch PUT: id={0}")
            @ValueSource(strings = ID_UPDATE_PUT)
            @DisplayName("Aendern eines vorhandenen Produktes durch PUT")
            void put(final String id) {
                // given
                final var responseGet = client
                    .get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .retrieve()
                    .toEntity(ProduktDownload.class)
                    .block();
                assertThat(responseGet).isNotNull();
                final var etag = responseGet.getHeaders().getETag();
                assertThat(etag)
                    .isNotNull()
                    .isNotEmpty();
                final var produktOrig = responseGet.getBody();
                assertThat(produktOrig).isNotNull();
                final var produkt = new ProduktDTO(
                    produktOrig.name(),
                    produktOrig.erscheinungsdatum(),
                    produktOrig.homepage(),
                    null,
                    null
                );

                // when
                final var statusCode = client
                    .put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, etag)
                    .bodyValue(produkt)
                    .exchangeToMono(response -> Mono.just(response.statusCode()))
                    .block();

                // then
                assertThat(statusCode).isEqualTo(NO_CONTENT);
            }

            @ParameterizedTest(name = "[{index}] Aendern durch Put mit ungueltigen Werten: id={0}, name={1}")
            @CsvSource(ID_UPDATE_PUT + ',' + NEUER_NAME_INVALID + ',')
            @DisplayName("Aendern durch Put mit ungueltigen Werten")
            void updateInvalid(final String id, final String name) {
                // given
                final var responseGet = client
                    .get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .retrieve()
                    .toEntity(ProduktDownload.class)
                    .block();
                assertThat(responseGet).isNotNull();
                final var etag = responseGet.getHeaders().getETag();
                assertThat(etag)
                    .isNotNull()
                    .isNotEmpty();
                final var produktOrig = responseGet.getBody();
                assertThat(produktOrig).isNotNull();
                final var produkt = new ProduktDTO(
                    name,
                    produktOrig.erscheinungsdatum(),
                    produktOrig.homepage(),
                    null,
                    null
                );
                final var violationKeys = List.of("name", "email", "kategorie");

                // when
                final var body = client
                    .put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, etag)
                    .bodyValue(produkt)
                    .exchangeToMono(response -> {
                        assertThat(response)
                            .extracting(ClientResponse::statusCode)
                            .isEqualTo(UNPROCESSABLE_ENTITY);
                        return response.bodyToMono(ProblemDetail.class);
                    })
                    .block();

                // then
                assertThat(body).isNotNull();
                final var detail = body.getDetail();
                assertThat(detail)
                    .isNotNull()
                    .isNotEmpty();
                @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
                final var violations = detail.split(", ");
                assertThat(violations)
                    .isNotNull()
                    .hasSize(violationKeys.size());
                final var actualViolationKeys = Arrays.stream(violations)
                    .map(violation -> violation.substring(0, violation.indexOf(": ")))
                    .toList();
                assertThat(actualViolationKeys).containsExactlyInAnyOrderElementsOf(violationKeys);
            }

            @ParameterizedTest(name = "[{index}] Aendern durch Put ohne Version: id={0}")
            @ValueSource(strings = {ID_VORHANDEN, ID_UPDATE_PUT, ID_UPDATE_PATCH})
            @DisplayName("Aendern durch Put ohne Version")
            void updateOhneVersion(final String id) {
                // given
                final var responseGet = client
                    .get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .retrieve()
                    .toEntity(ProduktDownload.class)
                    .block();
                assertThat(responseGet).isNotNull();
                final var produktOrig = responseGet.getBody();
                assertThat(produktOrig).isNotNull();
                final var produkt = new ProduktDTO(
                    produktOrig.name(),
                    produktOrig.erscheinungsdatum(),
                    produktOrig.homepage(),
                    null,
                    null
                );

                // when
                final var body = client
                    .put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(produkt)
                    .exchangeToMono(response -> {
                        assertThat(response)
                            .extracting(ClientResponse::statusCode)
                            .isEqualTo(PRECONDITION_REQUIRED);
                        return response.bodyToMono(ProblemDetail.class);
                    })
                    .block();

                // then
                assertThat(body)
                    .isNotNull()
                    .extracting(ProblemDetail::getDetail)
                    .isNotNull()
                    .isEqualTo("Versionsnummer fehlt");
            }
        }
    }
}
