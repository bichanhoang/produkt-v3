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

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import static com.acme.produkt.dev.DevConfig.DEV;
import static com.acme.produkt.entity.Produkt.NAME_PATTERN;
import static com.acme.produkt.rest.ProduktGetController.REST_PATH;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;

@Tag("integration")
@Tag("rest")
@Tag("rest_get")
@DisplayName("REST-Schnittstelle fuer GET-Requests")
@ExtendWith(SoftAssertionsExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_19, max = JAVA_20)
@SuppressWarnings("WriteTag")
class ProduktGetRestTest {
    static final String SCHEMA = "http";
    static final String HOST = "localhost";

    private static final String ID_VORHANDEN = "00000000-0000-0000-0000-000000000001";
    private static final String ID_VORHANDEN_PRODUKT = "00000000-0000-0000-0000-000000000001";
    private static final String ID_VORHANDEN_ANDERES_PRODUKT = "00000000-0000-0000-0000-000000000002";
    private static final String ID_NICHT_VORHANDEN = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    private static final String ANGESTELLTER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String NAME_TEIL = "a";
    private static final String NAME_PREFIX_A = "A";
    private static final String NAME_PREFIX_D = "D";
    private static final String ID_PATH = "/{id}";
    private static final String NAME_PARAM = "name";
    private static final String ANGESTELLTER_ID_PARAM = "angestellterId";

    private final String baseUrl;
    private final WebClient client;
    private final WebClient clientProdukt;
    private final ProduktRepository produktRepo;

    @InjectSoftAssertions
    private SoftAssertions softly;

    ProduktGetRestTest(@LocalServerPort final int port, final ApplicationContext ctx) {
        final var getController = ctx.getBean(ProduktGetController.class);
        assertThat(getController).isNotNull();

        final var uriComponents = UriComponentsBuilder.newInstance()
            .scheme(SCHEMA)
            .host(HOST)
            .port(port)
            .path(REST_PATH)
            .build();
        baseUrl = uriComponents.toUriString();
        client = WebClient
            .builder()
            .baseUrl(baseUrl)
            .build();
        clientProdukt = WebClient
            .builder()
            .baseUrl(baseUrl)
            .build();
        final var clientAdapter = WebClientAdapter.forClient(client);
        final var proxyFactory = HttpServiceProxyFactory
            .builder(clientAdapter)
            .build();
        produktRepo = proxyFactory.createClient(ProduktRepository.class);
    }

    @Test
    @DisplayName("Immer erfolgreich")
    void immerErfolgreich() {
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Noch nicht fertig")
    @Disabled
    void nochNichtFertig() {
        //noinspection DataFlowIssue
        assertThat(false).isTrue();
    }

    @Test
    @DisplayName("Suche nach allen Produkten")
    @SuppressWarnings("DataFlowIssue")
    void findAll() {
        // given
        final Map<String, String> suchkriterien = emptyMap();

        // when
        final var produkte = produktRepo.getProdukte(suchkriterien).block();

        // then
        softly.assertThat(produkte).isNotNull();
        final var embedded = produkte._embedded();
        softly.assertThat(embedded).isNotNull();
        softly.assertThat(embedded.produkte())
            .isNotNull()
            .isNotEmpty();
    }

    @ParameterizedTest(name = "[{index}] Suche mit vorhandenem (Teil-) Namen: teil={0}")
    @ValueSource(strings = NAME_TEIL)
    @DisplayName("Suche mit vorhandenem (Teil-) Namen")
    void findByNachnameTeil(final String teil) {
        // given
        final MultiValueMap<String, String> suchkriterien = new LinkedMultiValueMap<>();
        suchkriterien.add(NAME_PARAM, teil);

        // when
        final var produkte = produktRepo.getProdukte(suchkriterien.toSingleValueMap()).block();

        // then
        assertThat(produkte).isNotNull();
        final var embedded = produkte._embedded();
        assertThat(embedded).isNotNull();
        final var produkteList = embedded.produkte();
        assertThat(produkteList)
            .isNotNull()
            .isNotEmpty();
        produkteList
            .stream()
            .map(ProduktDownload::name)
            .forEach(name -> softly.assertThat(name).containsIgnoringCase(teil));
    }

    @ParameterizedTest(name = "[{index}] Suche mit vorhandenem Namen und PLZ: name={0}")
    @CsvSource(NAME_TEIL)
    @DisplayName("Suche mit vorhandenem Name")
    void findByNachname(final String name) {
        // given
        final MultiValueMap<String, String> suchkriterien = new LinkedMultiValueMap<>();
        suchkriterien.add(NAME_PARAM, name);

        // when
        final var produkte = produktRepo.getProdukte(suchkriterien.toSingleValueMap()).block();

        // then
        assertThat(produkte).isNotNull();
        assertThat(produkte._embedded()).isNotNull();
        final var produkteList = produkte._embedded().produkte();
        assertThat(produkteList)
            .isNotNull()
            .isNotEmpty();
        produkteList
            .forEach(produkt -> softly.assertThat(produkt.name()).containsIgnoringCase(name));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Nested
    @DisplayName("REST-Schnittstelle fuer die Suche anhand der ID")
    class FindById {
        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID und JsonPath: id={0}")
        @ValueSource(strings = ID_VORHANDEN)
        @DisplayName("Suche mit vorhandener ID und JsonPath")
        void findByIdJson(final String id) {
            // given

            // when
            final var body = client
                .get()
                .uri(ID_PATH, id)
                .accept(HAL_JSON)
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .block();

            // then
            assertThat(body).isNotNull().isNotBlank();

            final var namePath = "$.name";
            final String name = JsonPath.read(body, namePath);
            softly.assertThat(name).matches(NAME_PATTERN);

            final LinkDiscoverer linkDiscoverer = new HalLinkDiscoverer();
            final var selfLink = linkDiscoverer.findLinkWithRel("self", body).get().getHref();
            softly.assertThat(selfLink).isEqualTo(baseUrl + "/" + id);
        }

        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID: id={0}")
        @ValueSource(strings = ID_VORHANDEN)
        @DisplayName("Suche mit vorhandener ID")
        void findById(final String id) {
            // when
            final var produkt = produktRepo.getProdukt(id).block();

            // then
            assertThat(produkt).isNotNull();
            softly.assertThat(produkt.angestellterNachname())
                .isNotNull()
                .isNotBlank()
                .isNotEqualTo("N/A");

            softly.assertThat(produkt.angestellterEmail())
                .isNotNull()
                .isNotBlank()
                .isNotEqualTo("N/A");

            softly.assertThat(produkt._links().self().href())
                .isNotNull()
                .isNotBlank()
                .isEqualTo(baseUrl + '/' + id);
        }

        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID und vorhandener Version: id={0}, version={1}")
        @CsvSource(ID_VORHANDEN + ", 0")
        @DisplayName("Suche mit vorhandener ID und vorhandener Version")
        void findByIdVersionVorhanden(final String id, final String version) {
            // when
            final var statusCode = client
                .get()
                .uri(ID_PATH, id)
                .accept(HAL_JSON)
                .ifNoneMatch("\"" + version + '"')
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block();

            // then
            assertThat(statusCode).isEqualTo(NOT_MODIFIED);
        }

        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID und Version als produkt: id={0}, version={1}")
        @CsvSource(ID_VORHANDEN_PRODUKT + ", 0")
        @DisplayName("Suche mit vorhandener ID und Version als produkt")
        void findByIdVersionRolleProdukt(final String id, final String version) {
            // when
            final var statusCode = clientProdukt
                .get()
                .uri(ID_PATH, id)
                .accept(HAL_JSON)
                .ifNoneMatch("\"" + version + '"')
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block();

            // then
            assertThat(statusCode).isEqualTo(NOT_MODIFIED);
        }

        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID und Version unberechtigt: id={0}, version={1}")
        @CsvSource(ID_VORHANDEN_ANDERES_PRODUKT + ", 0")
        @DisplayName("Suche mit vorhandener ID und Version unberechtigt")
        void findByIdAnderesProdukt(final String id, final String version) {
            // when
            final var statusCode = clientProdukt
                .get()
                .uri(ID_PATH, id)
                .accept(HAL_JSON)
                .ifNoneMatch("\"" + version + '"')
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block();

            // then
            assertThat(statusCode).isEqualTo(FORBIDDEN);
        }

        @ParameterizedTest(name = "[{index}] Suche mit nicht-vorhandener ID: {0}")
        @ValueSource(strings = ID_NICHT_VORHANDEN)
        @DisplayName("Suche mit nicht-vorhandener ID")
        void findByIdNichtVorhanden(final String id) {
            // when
            final var statusCode = client
                .get()
                .uri(ID_PATH, id)
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block();

            // then
            assertThat(statusCode).isEqualTo(NOT_FOUND);
        }

        @ParameterizedTest(name = "[{index}] Suche mit nicht-vorhandener ID als Produkt: {0}")
        @ValueSource(strings = ID_NICHT_VORHANDEN)
        @DisplayName("Suche mit nicht-vorhandener ID als Produkt")
        void findByIdNichtVorhandesProdukt(final String id) {
            // when
            final var statusCode = clientProdukt
                .get()
                .uri(ID_PATH, id)
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block();

            // then
            assertThat(statusCode).isEqualTo(FORBIDDEN);
        }

    }

    @Nested
    @DisplayName("REST-Schnittstelle fuer die Suche nach Strings")
    class SucheNachStrings {
        @ParameterizedTest(name = "[{index}] Suche Namen mit Praefix prefix={0}")
        @ValueSource(strings = {NAME_PREFIX_A, NAME_PREFIX_D})
        @DisplayName("Suche Namen mit Praefix")
        void findNamen(final String prefix) {
            // when
            final var namenStr = client
                .get()
                .uri(builder -> builder.pathSegment(NAME_PARAM, prefix).build())
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .block();

            // then
            assertThat(namenStr)
                .isNotNull()
                .isNotEmpty();
            final var tmp = namenStr.replace(" ", "").substring(1);
            final var namen = tmp.substring(0, tmp.length() - 1).split(",");
            assertThat(namen)
                .isNotNull()
                .isNotEmpty();
            Arrays.stream(namen)
                .forEach(name -> assertThat(name).startsWith(prefix));
        }
    }

    @ParameterizedTest(name = "[{index}] Suche mit vorhandener Angestellter-ID: angestellterId={0}")
    @ValueSource(strings = ANGESTELLTER_ID)
    @DisplayName("Suche mit vorhandener Angestellter-ID")
    void findByKundeId(final String angestellterId) {
        // given
        final var suchkriterien = Map.of(ANGESTELLTER_ID_PARAM, angestellterId);

        // when
        final var produkte = produktRepo.getProdukte(suchkriterien).block();

        // then
        assertThat(produkte).isNotNull();
        final var embedded = produkte._embedded();
        assertThat(embedded).isNotNull();
        final var produkteEmbedded = embedded.produkte();
        assertThat(produkteEmbedded)
            .isNotNull()
            .isNotEmpty();
        produkteEmbedded
            .stream()
            .map(produkt -> produkt.angestellterId().toString().toLowerCase())
            .forEach(kid -> assertThat(kid).isEqualTo(angestellterId));
    }
}
