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
package com.acme.kunde.repository;

import com.acme.kunde.entity.Adresse_;
import com.acme.kunde.entity.FamilienstandType;
import com.acme.kunde.entity.GeschlechtType;
import com.acme.kunde.entity.InteresseType;
import com.acme.kunde.entity.Kunde;
import com.acme.kunde.entity.Kunde_;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Singleton-Klasse, um Specifications für Queries in Spring Data zu bauen.
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@Component
@Slf4j
public class SpecBuilder {
    /**
     * Specification für eine Query mit Spring Data bauen.
     *
     * @param queryParams als MultiValueMap
     * @return Specification für eine Query mit Spring Data
     */
    public Optional<Specification<Kunde>> build(final Map<String, ? extends List<String>> queryParams) {
        log.debug("build: queryParams={}", queryParams);

        if (queryParams.isEmpty()) {
            // keine Suchkriterien
            return Optional.empty();
        }

        final var specs = queryParams
            .entrySet()
            .stream()
            .map(entry -> toSpec(entry.getKey(), entry.getValue()))
            .toList();

        if (specs.isEmpty() || specs.contains(null)) {
            return Optional.empty();
        }

        return Optional.of(Specification.allOf(specs));
    }

    @SuppressWarnings("CyclomaticComplexity")
    private Specification<Kunde> toSpec(final String paramName, final List<String> paramValues) {
        log.trace("toSpec: paramName={}, paramValues={}", paramName, paramValues);
        if (Objects.equals(paramName, "interesse")) {
            return toSpecInteressen(paramValues);
        }

        if (paramValues == null || paramValues.size() != 1) {
            return null;
        }

        final var value = paramValues.get(0);
        return switch (paramName) {
            case "nachname" -> nachname(value);
            case "email" ->  email(value);
            case "kategorie" -> kategorie(value);
            case "newsletter" -> newsletter(value);
            case "geschlecht" -> geschlecht(value);
            case "familienstand" -> familienstand(value);
            case "plz" -> plz(value);
            case "ort" -> ort(value);
            default -> null;
        };
    }

    private Specification<Kunde> toSpecInteressen(final Collection<String> interessen) {
        log.trace("build: interessen={}", interessen);
        if (interessen == null || interessen.isEmpty()) {
            return null;
        }

        final var specsImmutable = interessen.stream()
            .map(this::interesse)
            .toList();
        if (specsImmutable.isEmpty() || specsImmutable.contains(null)) {
            return null;
        }

        final List<Specification<Kunde>> specs = new ArrayList<>(specsImmutable);
        final var first = specs.remove(0);
        return specs.stream().reduce(first, Specification::and);
    }

    private Specification<Kunde> nachname(final String teil) {
        // root ist jakarta.persistence.criteria.Root<Kunde>
        // query ist jakarta.persistence.criteria.CriteriaQuery<Kunde>
        // builder ist jakarta.persistence.criteria.CriteriaBuilder
        // https://www.logicbig.com/tutorials/java-ee-tutorial/jpa/meta-model.html
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.like(
                builder.lower(root.get(Kunde_.nachname)),
                builder.lower(builder.literal("%" + teil + '%'))
            );
        };
    }

    private Specification<Kunde> email(final String teil) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.like(
                builder.lower(root.get(Kunde_.email)),
                builder.lower(builder.literal("%" + teil + '%'))
            );
        };
    }

    private Specification<Kunde> kategorie(final String kategorie) {
        final int kategorieInt;
        try {
            kategorieInt = Integer.parseInt(kategorie);
        } catch (final NumberFormatException e) {
            //noinspection ReturnOfNull
            return null;
        }
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.equal(root.get(Kunde_.kategorie), kategorieInt);
        };
    }

    private Specification<Kunde> newsletter(final String hasNewsletter) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.equal(
                root.get(Kunde_.hasNewsletter),
                Boolean.parseBoolean(hasNewsletter)
            );
        };
    }

    private Specification<Kunde> geschlecht(final String geschlecht) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.equal(
                root.get(Kunde_.geschlecht),
                GeschlechtType.of(geschlecht).orElse(null)
            );
        };
    }

    private Specification<Kunde> familienstand(final String familienstand) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.equal(
                root.get(Kunde_.familienstand),
                FamilienstandType.of(familienstand).orElse(null)
            );
        };
    }

    private Specification<Kunde> interesse(final String interesse) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.isMember(
                InteresseType.of(interesse).orElse(null),
                root.get(Kunde_.interessen)
            );
        };
    }

    private Specification<Kunde> plz(final String prefix) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.like(root.get(Kunde_.adresse).get(Adresse_.plz), prefix + '%');
        };
    }

    private Specification<Kunde> ort(final String prefix) {
        return (root, query, builder) -> {
            root.fetch(Kunde_.adresse);
            root.fetch(Kunde_.interessen);
            return builder.like(
                builder.lower(root.get(Kunde_.adresse).get(Adresse_.ort)),
                builder.lower(builder.literal(prefix + '%'))
            );
        };
    }
}
