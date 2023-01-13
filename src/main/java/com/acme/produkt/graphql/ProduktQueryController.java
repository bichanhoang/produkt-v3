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
package com.acme.produkt.graphql;

import com.acme.produkt.entity.Produkt;
import com.acme.produkt.service.ProduktReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;

/**
 * Eine Controller-Klasse für das Lesen mit der GraphQL-Schnittstelle und den Typen aus dem GraphQL-Schema.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@h-ka.de)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
final class ProduktQueryController {
    private final ProduktReadService service;

    /**
     * Suche anhand der Produkt-ID.
     *
     * @param id ID des zu suchenden Produktes
     *
     * @return Das gefundene Produkt
     */
    @QueryMapping
    Produkt produkt(@Argument final UUID id) {
        log.debug("findById: id={}", id);
        final var produkt = service.findById(id);
        log.debug("findById: {}", produkt);
        return produkt;
    }

    /**
     * Suche mit diversen Suchkriterien.
     *
     * @param input Suchkriterien und ihre Werte, z.B. `name` und `Alpha`
     * @return Die gefundenen Produkte als Collection
     */
    @QueryMapping
    Collection<Produkt> produkte(@Argument final Optional<Suchkriterien> input) {
        log.debug("produkte: input={}", input);
        final var suchkriterien = input.map(Suchkriterien::toMap).orElse(emptyMap());
        final var produkte = service.find(suchkriterien);
        log.debug("produkte: {}", produkte);
        return produkte;
    }
}
