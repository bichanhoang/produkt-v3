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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.produkt.service;

import com.acme.produkt.entity.Produkt;
import com.acme.produkt.repository.ProduktRepository;
import com.acme.produkt.repository.SpecBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anwendungslogik für Produkt.
 * <img src="../../../../../asciidoc/ProduktReadService.svg" alt="Klassendiagramm">
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class ProduktReadService {
    private final ProduktRepository repo;

    private final SpecBuilder specBuilder;

    /**
     * Ein Produkt anhand seiner ID suchen.
     *
     * @param id Die Id des gesuchten Produktes
     * @return Das gefundene Produkt
     * @throws NotFoundException Falls kein Produkt gefunden wurde
     * */

    public @NonNull Produkt findById(final UUID id) {
        log.debug("findById: id={}", id);
        final var produktOpt = repo.findById(id);

        // admin: Kundendaten evtl. nicht gefunden
        final var produkt = produktOpt.orElseThrow(() -> new NotFoundException(id));
        log.debug("findById: {}", produkt);
        return produkt;
    }

    /**
     * Produkte anhand von Suchkriterien als Collection suchen.
     *
     * @param suchkriterien Die Suchkriterien
     * @return Die gefundenen Produkte oder eine leere Liste
     * @throws NotFoundException Falls keine Produkte gefunden wurden
     */
    @SuppressWarnings({"ReturnCount", "NestedIfDepth"})
    public @NonNull Collection<Produkt> find(@NonNull final Map<String, List<String>> suchkriterien) {
        log.debug("find: suchkriterien={}", suchkriterien);

        if (suchkriterien.isEmpty()) {
            return repo.findAll();
        }

        if (suchkriterien.size() == 1) {
            final var namen = suchkriterien.get("name");
            if (namen != null && namen.size() == 1) {
                final var produkte = repo.findByName(namen.get(0));
                if (produkte.isEmpty()) {
                    throw new NotFoundException(suchkriterien);
                }
                log.debug("find (name): {}", produkte);
                return produkte;
            }
        }

        final var spec = specBuilder
            .build(suchkriterien)
            .orElseThrow(() -> new NotFoundException(suchkriterien));
        final var produkte = repo.findAll(spec);
        if (produkte.isEmpty()) {
            throw new NotFoundException(suchkriterien);
        }
        log.debug("find: {}", produkte);
        return produkte;
    }

    /**
     * Abfrage, welche Namen es zu einem Präfix gibt.
     *
     * @param prefix Name-Präfix.
     * @return Die passenden Namen.
     * @throws NotFoundException Falls keine Namen gefunden wurden.
     */
    public Collection<String> findNamenByPrefix(final String prefix) {
        log.debug("findNamenByPrefix: {}", prefix);
        final var namen = repo.findNamenByPrefix(prefix);
        if (namen.isEmpty()) {
            throw new NotFoundException();
        }
        log.debug("findNamenByPrefix: {}", namen);
        return namen;
    }
}