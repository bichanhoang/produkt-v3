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
package com.acme.produkt.service;
import com.acme.produkt.entity.Produkt;
import com.acme.produkt.entity.Umsatz;
import com.acme.produkt.repository.AngestellterRepository;
import com.acme.produkt.repository.ProduktRepository;
import com.acme.produkt.repository.SpecBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static java.math.BigDecimal.ONE;
import static java.time.LocalDateTime.now;
import static java.util.Locale.GERMANY;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;
import static org.mockito.Mockito.when;

@Tag("unit")
@Tag("service_read")
@DisplayName("Anwendungskern fuer Lesen")
@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@EnabledForJreRange(min = JAVA_19, max = JAVA_20)
@SuppressWarnings({"ClassFanOutComplexity", "InnerTypeLast", "WriteTag"})
class ProduktReadServiceTest {
    private static final String ID_VORHANDEN = "00000000-0000-0000-0000-000000000001";
    private static final String ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999";
    private static final String NAME = "Name-Test";
    private static final LocalDate ERSCHEINUNGSDATUM = LocalDate.of(2022, 1, 1);
    private static final Currency WAEHRUNG = Currency.getInstance(GERMANY);
    private static final String HOMEPAGE = "https://test.de";

    @Mock
    private ProduktRepository repo;

    private AngestellterRepository angestellterRepo;

    private final SpecBuilder specBuilder = new SpecBuilder();

    private ProduktReadService service;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void beforeEach() {
        service = new ProduktReadService(repo, angestellterRepo, specBuilder);
    }

    @Test
    @DisplayName("Immer erfolgreich")
    void immerErfolgreich() {
        assertThat(true).isTrue();
    }

    @Test
    @Disabled
    @DisplayName("Noch nicht fertig")
    void nochNichtFertig() {
        //noinspection DataFlowIssue
        assertThat(false).isTrue();
    }

    @ParameterizedTest(name = "Suche alle Produkte")
    @ValueSource(strings = NAME)
    @DisplayName("Suche alle Produkte")
    void findAll(final String name) {
        // given
        final var produkt = createProduktMock(name);
        final var produkteMock = List.of(produkt);
        when(repo.findAll()).thenReturn(produkteMock);
        final Map<String, List<String>> keineSuchkriterien = new LinkedMultiValueMap<>();

        // when
        final var produkte = service.find(keineSuchkriterien);

        // then
        assertThat(produkte).isNotEmpty();
    }

    @ParameterizedTest(name = "[{index}] Suche mit vorhandenem Namen: name={0}")
    @ValueSource(strings = NAME)
    @DisplayName("Suche mit vorhandenem Namen")
    void findByName(final String name) {
        // given
        final var produkt = createProduktMock(name);
        final var produktMock = List.of(produkt);
        when(repo.findByName(name)).thenReturn(produktMock);
        final MultiValueMap<String, String> suchkriterien = new LinkedMultiValueMap<>();
        suchkriterien.add("name", name);

        // when
        final var produkte = service.find(suchkriterien);

        // then
        assertThat(produkte)
            .isNotNull()
            .isNotEmpty();
        produkte
            .stream()
            .map(Produkt::getName)
            .forEach(nameProdukt -> softly.assertThat(nameProdukt).containsIgnoringCase(name));
    }

    @Nested
    @DisplayName("Anwendungskern fuer die Suche anhand der ID")
    class FindById {
        @ParameterizedTest(name = "[{index}] Suche mit vorhandener ID: id={0}")
        @CsvSource(ID_VORHANDEN + ',' + NAME + ',')
        @DisplayName("Suche mit vorhandener ID")
        void findById(final String idStr, final String name) {
            // given
            final var id = UUID.fromString(idStr);
            final var produktMock = createProduktMock(id, name);
            when(repo.findById(id)).thenReturn(Optional.of(produktMock));

            // when
            final var produkt = service.findById(id);

            // then
            assertThat(produkt.getId()).isEqualTo(produktMock.getId());
        }

        @ParameterizedTest(name = "[{index}] Suche mit nicht vorhandener ID: id={0}")
        @ValueSource(strings = ID_NICHT_VORHANDEN)
        @DisplayName("Suche mit nicht vorhandener ID")
        void findByIdNichtVorhanden(final String idStr) {
            // given
            final var id = UUID.fromString(idStr);
            when(repo.findById(id)).thenReturn(Optional.empty());

            // when
            final var notFoundException = catchThrowableOfType(
                () -> service.findById(id),
                NotFoundException.class
            );

            // then
            assertThat(notFoundException)
                .isNotNull()
                .extracting(NotFoundException::getId)
                .isEqualTo(id);
        }
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden fuer Mock-Objekte
    // -------------------------------------------------------------------------
    private Produkt createProduktMock(final String name) {
        return createProduktMock(randomUUID(), name);
    }

    // wird auch fuer WriteService verwendet
    private Produkt createProduktMock(
        final UUID id,
        final String name
    ) {
        final URL homepage;
        try {
            homepage = URI.create(HOMEPAGE).toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        final var umsatz = Umsatz.builder()
            .id(randomUUID())
            .betrag(ONE)
            .waehrung(WAEHRUNG)
            .build();
        return Produkt.builder()
            .id(id)
            .version(0)
            .name(name)
            .erscheinungsdatum(ERSCHEINUNGSDATUM)
            .homepage(homepage)
            .umsatz(umsatz)
            .erzeugt(now(ZoneId.of("Europe/Berlin")))
            .aktualisiert(now(ZoneId.of("Europe/Berlin")))
            .build();
    }
}
