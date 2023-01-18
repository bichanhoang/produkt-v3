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
package com.acme.produkt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

/**
 * Daten eines Produktes. In DDD ist Produkt ist ein Aggregate Root.
 * <img src="../../../../../asciidoc/Produkt.svg" alt="Klassendiagramm">
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
// https://thorben-janssen.com/java-records-hibernate-jpa
@Entity
@Table(name = "produkt")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@ToString
@SuppressWarnings({"ClassFanOutComplexity", "JavadocDeclaration", "RequireEmptyLineBeforeBlockTagGroup"})
@Builder

public class Produkt {

    /**
     * Muster für einen gültigen Namen.
     */
    public static final String NAME_PATTERN = "[A-ZÄÖÜ][a-zäöüß]+(-[A-ZÄÖÜ][a-zäöüß]+)?";

    private static final int MAX_LENGTH = 40;

    /**
     * Die ID des Produktes.
     * @param id Die ID.
     * @return Die ID.
     */
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Versionsnummer für optimistische Synchronisation.
     */
    @Version
    private int version;

    /**
     * Der Name des Produktes.
     * @param name Der name.
     * @return Der name.
     */
    @NotNull
    @Pattern(regexp = NAME_PATTERN)
    @Size(max = MAX_LENGTH)
    private String name;

    /**
     * Das Erscheinungsdatum des Produktes.
     * @param email Das Erscheinungsdatum.
     * @return Das Erscheinungsdatum.
     */
    @Past
    @Column(length = MAX_LENGTH)
    private LocalDate erscheinungsdatum;

    /**
     * Die URL zur Homepage des Produktes.
     * @param homepage Die URL zur Homepage.
     * @return Die URL zur Homepage.
     */
    private URL homepage;

    /**
     * Der Umsatz des Produktes.
     * @param umsatz Der Umsatz.
     * @return Der Umsatz.
     */
    @OneToOne(cascade = {PERSIST, REMOVE}, fetch = LAZY)
    @JoinColumn(updatable = false)
    @ToString.Exclude
    private Umsatz umsatz;

    // der Spaltenwert referenziert einen Wert aus einer anderen DB
    @Column(name = "angestellter_id")
    private UUID angestellterId;

    @CreationTimestamp
    private LocalDateTime erzeugt;

    @UpdateTimestamp
    private LocalDateTime aktualisiert;

    @Transient
    private String angestellterNachname;

    @Transient
    private String angestellterEmail;

    /**
     * Produktdaten überschreiben.
     *
     * @param produkt Neue Produktdaten.
     */
    public void set(final Produkt produkt) {
        name = produkt.name;
        erscheinungsdatum = produkt.erscheinungsdatum;
        homepage = produkt.homepage;
    }
}
