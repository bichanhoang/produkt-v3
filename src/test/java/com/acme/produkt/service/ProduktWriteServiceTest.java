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

import com.acme.produkt.MailProps;
import com.acme.produkt.entity.Produkt;
import com.acme.produkt.entity.Umsatz;
import com.acme.produkt.repository.ProduktRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import static java.math.BigDecimal.ONE;
import static java.time.LocalDateTime.now;
import static java.util.Locale.GERMANY;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;
import static org.mockito.Mockito.when;

// https://junit.org/junit5/docs/current/user-guide
// https://assertj.github.io/doc

@Tag("unit")
@Tag("service_write")
@DisplayName("Anwendungskern fuer Schreiben")
@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@EnabledForJreRange(min = JAVA_19, max = JAVA_20)
@SuppressWarnings({"InnerTypeLast", "ClassFanOutComplexity", "MethodOnlyUsedFromInnerClass", "WriteTag"})
class ProduktWriteServiceTest {
    private static final String ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999";
    private static final String ID_UPDATE = "00000000-0000-0000-0000-000000000002";
    private static final String NAME = "Name-Test";
    private static final LocalDate ERSCHEINUNGSDATUM = LocalDate.of(2022, 1, 1);
    private static final Currency WAEHRUNG = Currency.getInstance(GERMANY);
    private static final String HOMEPAGE = "https://test.de";
    private static final String VERSION_ALT = "-1";

    @Mock
    private ProduktRepository repo;

    private final Validator validator;

    @Mock
    @SuppressWarnings({"unused", "UnusedVariable"})
    private DelegatingPasswordEncoder passwordEncoder;

    @Mock
    @SuppressWarnings({"unused", "UnusedVariable"})
    private JavaMailSender mailSender;

    @Mock
    @SuppressWarnings({"unused", "UnusedVariable"})
    private MailProps mailProps;

    private ProduktWriteService service;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @SuppressWarnings("RedundantModifier")
    ProduktWriteServiceTest() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @BeforeEach
    void beforeEach() {
        service = new ProduktWriteService(repo, validator);
    }

    @Nested
    @DisplayName("Anwendungskern fuer Erzeugen")
    class Erzeugen {
        @ParameterizedTest(name = "[{index}] Neuanlegen eines neuen Produktes: name={0}")
        @CsvSource(NAME)
        @DisplayName("Neuanlegen eines neuen Produktes")
        void create(final ArgumentsAccessor args) {
            // given
            final var name = args.getString(0);

            final var produktMock = createProduktMock(randomUUID(), name);
            when(repo.save(produktMock)).thenReturn(produktMock);

            // when
            final var produkte = service.create(produktMock);

            // then
            assertThat(produkte).isNotNull();
            softly.assertThat(produkte.getId()).isNotNull();
            softly.assertThat(produkte.getName()).isEqualTo(name);
        }

    }

    @Nested
    @DisplayName("Anwendungskern fuer Aendern")
    class Aendern {
        @ParameterizedTest(name = "[{index}] Aendern eines Produktes: id={0}, name={1}")
        @CsvSource(ID_UPDATE + ',' + NAME)
        @DisplayName("Aendern eines Produktes")
        void update(final String idStr, final String name) {
            // given
            final var id = UUID.fromString(idStr);
            final var produktMock = createProduktMock(id, name);
            when(repo.findById(id)).thenReturn(Optional.of(produktMock));
            when(repo.save(produktMock)).thenReturn(produktMock);

            // when
            final var produkte = service.update(produktMock, id, produktMock.getVersion());

            // then
            assertThat(produkte)
                .isNotNull()
                .extracting(Produkt::getId)
                .isEqualTo(produktMock.getId());
        }

        @ParameterizedTest(name = "[{index}] Aendern eines nicht-vorhandenen Produktes: id={0}, name={1}")
        @CsvSource(ID_NICHT_VORHANDEN + ',' + NAME)
        @DisplayName("Aendern eines nicht-vorhandenen Produktes")
        void updateNichtVorhanden(final String idStr, final String name) {
            // given
            final var id = UUID.fromString(idStr);
            final var produktMock = createProduktMock(id, name);
            when(repo.findById(id)).thenReturn(Optional.empty());

            // when
            final var notFoundException = catchThrowableOfType(
                () -> service.update(produktMock, id, produktMock.getVersion()),
                NotFoundException.class
            );

            // then
            assertThat(notFoundException).isNotNull();
        }

        @ParameterizedTest(name = "[{index}] Aendern mit alter Versionsnummer: id={0}, version={4}")
        @CsvSource(ID_UPDATE + ',' + NAME + ',' + VERSION_ALT)
        @DisplayName("Aendern mit alter Versionsnummer")
        void updateVersionOutdated(final ArgumentsAccessor args) {
            // given
            final var idStr = args.getString(0);
            final var id = UUID.fromString(idStr);
            final var name = args.getString(1);
            final var version = args.getInteger(2);
            final var produktMock = createProduktMock(id, name);
            when(repo.findById(id)).thenReturn(Optional.of(produktMock));

            // when
            @SuppressWarnings("LocalVariableNamingConvention")
            final var versionOutdatedException = catchThrowableOfType(
                () -> service.update(produktMock, id, version),
                VersionOutdatedException.class
            );

            // then
            assertThat(versionOutdatedException)
                .isNotNull()
                .extracting(VersionOutdatedException::getVersion)
                .isEqualTo(version);
        }
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden fuer Mock-Objekte
    // -------------------------------------------------------------------------
    private Produkt createProduktMock(final UUID id, final String name) {
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
