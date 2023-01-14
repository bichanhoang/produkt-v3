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
package com.acme.produkt.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlTransportException;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;
import java.util.UUID;

/**
 * REST- oder GraphQL-Client für Kundedaten.
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AngestellterRepository {
    private final AngestellterRestRepository angestellterRestRepository;
    private final HttpGraphQlClient graphQlClient;

    /**
     * Angestellter anhand der Angestellter-ID suchen.
     *
     * @param angestellterId Die Id des gesuchten Angestellter.
     * @return Der gefundene Kunde oder null.
     * @throws AngestellterServiceException falls beim Zugriff auf den Web Service eine Exception eingetreten ist.
     */
    public Optional<Angestellter> findById(final UUID angestellterId) {
        log.debug("findById: angestellterId={}", angestellterId);

        final Angestellter angestellter;
        try {
            angestellter = angestellterRestRepository.getAngestellter(angestellterId.toString()).block();
        } catch (final WebClientResponseException.NotFound ex) {
            log.error("findById: WebClientResponseException.NotFound");
            return Optional.empty();
        } catch (final WebClientException ex) {
            // WebClientRequestException oder WebClientResponseException (z.B. ServiceUnavailable)
            log.error("findById: {}", ex.getClass().getSimpleName());
            throw new AngestellterServiceException(ex);
        }

        log.debug("findById: {}", angestellter);
        return Optional.ofNullable(angestellter);
    }

    /**
     * Die Emailadresse anhand der Angestellter-ID suchen.
     *
     * @param angestellterId Die Id des gesuchten Angestellten.
     * @return Die Emailadresse in einem Optional oder ein leeres Optional.
     * @throws AngestellterServiceException falls beim Zugriff auf den Web Service eine Exception eingetreten ist.
     */
    public Optional<String> findEmailById(final UUID angestellterId) {
        log.debug("findEmailById: angestellterId={}", angestellterId);
        final var query = """
            query {
                kunde(id: "%s") {
                    email
                }
            }
            """.formatted(angestellterId);

        final String email;
        try {
            email = graphQlClient.document(query)
                .retrieve("angestellter")
                .toEntity(EmailEntity.class)
                .map(EmailEntity::email)
                .block();
        } catch (final FieldAccessException ex) {
            log.warn("findEmailById: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (final GraphQlTransportException ex) {
            log.warn("findEmailById: {}", ex.getClass().getSimpleName());
            throw new AngestellterServiceException(ex);
        }

        log.debug("findEmailById: {}", email);
        return Optional.ofNullable(email);
    }
}
