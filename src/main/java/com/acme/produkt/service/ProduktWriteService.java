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
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Anwendungslogik für Produkte auch mit Bean Validation.
 * ![Klassendiagramm](../../../images/KundeWriteService.svg)
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProduktWriteService {
    private final ProduktRepository repo;

    // https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#validation-beanvalidation
    private final Validator validator;

    /**
     * Einen neues Produkt anlegen.
     *
     * @param produkt Das Objekt des neu anzulegenden Produktes.
     * @return Das neu angelegte Produkt mit generierter ID.
     * @throws ConstraintViolationsException Falls mindestens ein Constraint verletzt ist.
     */
    @Transactional
    @SuppressWarnings("TrailingComment")
    public Produkt create(final Produkt produkt) {
        log.debug("create: {}", produkt); //NOSONAR

        final var violations = validator.validate(produkt);
        if (!violations.isEmpty()) {
            log.debug("create: violations={}", violations);
            throw new ConstraintViolationsException(violations);
        }

        final var produktDB = repo.save(produkt);

        log.debug("create: {}", produktDB);
        return produktDB;
    }

    /**
     * Ein vorhandenes Produkt aktualisieren.
     *
     * @param produkt Das Objekt mit den neuen Daten (ohne ID)
     * @param id ID des zu aktualisierenden Produktes
     * @throws ConstraintViolationsException Falls mindestens ein Constraint verletzt ist.
     * @throws NotFoundException Kein Produkt zur ID vorhanden.
     */
    @Transactional
    public Produkt update(final Produkt produkt, final UUID id, final int version) {
        log.debug("update: {}", produkt);
        log.debug("update: id={}, version={}", id, version);

        final var violations = validator.validate(produkt);
        if (!violations.isEmpty()) {
            log.debug("update: violations={}", violations);
            throw new ConstraintViolationsException(violations);
        }
        log.trace("update: Keine Constraints verletzt");

        final var produktDbOptional = repo.findById(id);
        if (produktDbOptional.isEmpty()) {
            throw new NotFoundException(id);
        }

        var produktDb = produktDbOptional.get();
        log.trace("update: version={}, produktDb={}", version, produktDb);
        if (version != produktDb.getVersion()) {
            throw new VersionOutdatedException(version);
        }

        produktDb.set(produkt);
        produktDb = repo.save(produktDb);
        log.debug("update: {}", produktDb);
        return produktDb;
    }
}
