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
package com.acme.produkt.repository;

import com.acme.produkt.entity.Produkt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository für den DB-Zugriff bei Produkte.
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@Repository
public interface ProduktRepository extends JpaRepository<Produkt, UUID>, JpaSpecificationExecutor<Produkt> {
    @EntityGraph(attributePaths = {"adresse", "interessen"})

    @Override
    List<Produkt> findAll();

    @EntityGraph(attributePaths = {"adresse", "interessen"})
    @Override
    Optional<Produkt> findById(UUID id);

    /**
     * Produkte anhand des Namens suchen.
     *
     * @param name Der (Teil-) Name der gesuchten Produkte
     * @return Die gefundenen Produkte oder eine leere Collection
     */
    @Query("""
        SELECT   p
        FROM     Produkt p
        WHERE    lower(p.name) LIKE concat('%', lower(:name), '%')
        ORDER BY p.id
        """)
    @EntityGraph
    Collection<Produkt> findByName(CharSequence name);
    // https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#appendix.query.method.subject
    // Collection<Produkt> findByNameContainingIgnoreCaseOrderByIdAsc(CharSequence name);

    /**
     * Abfrage, welche Namen es zu einem Präfix gibt.
     *
     * @param prefix Name-Präfix.
     * @return Die passenden Namen oder eine leere Collection.
     */
    @Query("""
        SELECT DISTINCT p.name
        FROM     Produkt p
        WHERE    lower(p.name) LIKE concat(lower(:prefix), '%')
        ORDER BY p.name
        """)
    Collection<String> findNamenByPrefix(String prefix);

    /**
     * Produkte zu gegebener Angestellter-ID aus der DB ermitteln.
     *
     * @param angestellterId Angestellter-ID für die Suche
     * @return Liste der gefundenen Produkte
     */
    @EntityGraph
    List<Produkt> findByAngestellterId(UUID angestellterId);
}
