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
package com.acme.produkt.rest;

import com.acme.produkt.entity.Produkt;
import com.acme.produkt.service.ProduktReadService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.acme.produkt.service.NotFoundException;
import static com.acme.produkt.rest.ProduktGetController.REST_PATH;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;


/**
 * Eine @RestController-Klasse bildet die REST-Schnittstelle, wobei die HTTP-Methoden, Pfade und MIME-Typen auf die
 * Methoden der Klasse abgebildet werden.
 * <img src="../../../../../asciidoc/ProduktGetController.svg" alt="Klassendiagramm">
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@RestController
@RequestMapping(REST_PATH)
@OpenAPIDefinition(info = @Info(title = "Produkt API", version = "v2"))
@RequiredArgsConstructor
@SuppressWarnings("ClassFanOutComplexity")
@Slf4j
final class ProduktGetController {

    /**
     * Basispfad für die REST-Schnittstelle.
     */
    static final String REST_PATH = "/rest";

    /**
     * Muster für eine UUID. `$HEX_PATTERN{8}-($HEX_PATTERN{4}-){3}$HEX_PATTERN{12}` enthält eine _capturing group_
     * und ist nicht zulässig.
     */
    static final String ID_PATTERN =
        "[\\dA-Fa-f]{8}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{12}";

    /**
     * Pfad, um Nachnamen abzufragen.
     */
    @SuppressWarnings("TrailingComment")
    private static final String NAME_PATH = "/name"; //NOSONAR

    private final ProduktReadService service;

    private final UriHelper uriHelper;

    /**
     * Suche anhand der Produkt-ID als Pfad-Parameter.
     *
     * @param id ID des zu suchenden Produktes.
     * @param version Die Version des zu suchenden Produktes.
     * @param request Das Request-Objekt, um Links für HATEOAS zu erstellen.
     * @return Gefundenes Produkt mit Atom-Links.
     */
    @GetMapping(path = "{id:" + ID_PATTERN + "}", produces = HAL_JSON_VALUE)
    @Operation(summary = "Suche mit der Produkt-ID", tags = "Suchen")
    @ApiResponse(responseCode = "200", description = "Produkt gefunden")
    @ApiResponse(responseCode = "404", description = "Produkt nicht gefunden")
    ResponseEntity<ProduktModel> findById(
        @PathVariable final UUID id,
        @RequestHeader("If-None-Match") final Optional<String> version,
        final HttpServletRequest request) {

        // Anwendungskern
        final var produkt = service.findById(id);
        log.debug("findById: {}", produkt);

        final var currentVersion = "\"" + produkt.getVersion() + '"';
        if (Objects.equals(version.orElse(null), currentVersion)) {
            return status(NOT_MODIFIED).build();
        }

        final var model = produktToModel(produkt, request);
        log.debug("findById: model={}", model);
        return ok().eTag(currentVersion).body(model);
    }

    private ProduktModel produktToModel(final Produkt produkt, final HttpServletRequest request) {
        final var model = new ProduktModel(produkt);
        final var baseUri = uriHelper.getBaseUri(request).toString();
        final var idUri = baseUri + '/' + produkt.getId();

        final var selfLink = Link.of(idUri);
        final var listLink = Link.of(baseUri, LinkRelation.of("list"));
        final var addLink = Link.of(baseUri, LinkRelation.of("add"));
        final var updateLink = Link.of(idUri, LinkRelation.of("update"));
        final var removeLink = Link.of(idUri, LinkRelation.of("remove"));
        model.add(selfLink, listLink, addLink, updateLink, removeLink);
        return model;
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter.
     *
     * @param suchkriterien Query-Parameter als Map.
     * @param request Das Request-Objekt, um Links für HATEOAS zu erstellen.
     * @return Gefundene Produkte als CollectionModel.
     */
    @GetMapping(produces = HAL_JSON_VALUE)
    @Operation(summary = "Suche mit Suchkriterien", tags = "Suchen")
    @ApiResponse(responseCode = "200", description = "CollectionModel mit dem Produkten")
    @ApiResponse(responseCode = "404", description = "Keine Produkte gefunden")
    ResponseEntity<CollectionModel<? extends ProduktModel>> find(
        @RequestParam @NonNull final Map<String, String> suchkriterien,
        final HttpServletRequest request
    ) {
        log.debug("find: suchkriterien={}", suchkriterien);

        final Collection<Produkt> produkte;
        if (suchkriterien.isEmpty()) {
            produkte = service.findAll();
        } else {
            final var angestellterIdStr = suchkriterien.get("angestellterId");
            if (angestellterIdStr == null || suchkriterien.size() > 1) {
                return notFound().build();
            }
            final var angestellterId = UUID.fromString(angestellterIdStr);
            produkte = service.findByAngestellterId(angestellterId);
        }

        final var baseUri = uriHelper.getBaseUri(request).toString();
        final var models = produkte
            .stream()
            .map(produkt -> {
                final var model = new ProduktModel(produkt);
                model.add(Link.of(baseUri + '/' + produkt.getId()));
                return model;
            })
            .toList();
        log.debug("find: {}", models);

        if (models.isEmpty()) {
            return notFound().build();
        }

        return ok(CollectionModel.of(models));
    }

    /**
     * Abfrage, welche Namen es zu einem Präfix gibt.
     *
     * @param prefix Name-Präfix als Pfadvariable.
     * @return Die passenden Namen oder Statuscode 404, falls es keine gibt.
     */
    @GetMapping(path = NAME_PATH + "/{prefix}", produces = HAL_JSON_VALUE)
    String findNamenByPrefix(@PathVariable final String prefix) {
        log.debug("findNamenByPrefix: {}", prefix);
        final var namen = service.findNamenByPrefix(prefix);
        log.debug("findNamenByPrefix: {}", namen);
        return namen.toString();
    }

    /**
     * Für den Fall, dass die GET-Request keine Ergebnisse liefert, wird eine NotFoundException behandelt.
     *
     * @param ex Die NotFoundException, die gefangen wurde.
     */
    @ExceptionHandler
    @ResponseStatus(NOT_FOUND)
    void onNotFound(final NotFoundException ex) {
        log.debug("onNotFound: {}", ex.getMessage());
    }
}
