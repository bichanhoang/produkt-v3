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
package com.acme.kunde.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.UniqueElements;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

/**
 * Daten eines Kunden. In DDD ist Kunde ist ein Aggregate Root.
 * <img src="../../../../../asciidoc/Kunde.svg" alt="Klassendiagramm">
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
// https://thorben-janssen.com/java-records-hibernate-jpa
@Entity
@Table(name = "kunde")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@ToString
@Builder
@SuppressWarnings({
    "ClassFanOutComplexity",
    "RequireEmptyLineBeforeBlockTagGroup",
    "DeclarationOrder",
    "MagicNumber",
    "JavadocDeclaration",
    "MissingSummary",
    "RedundantSuppression"})
public class Kunde {
    /**
     * Muster für einen gültigen Nachnamen.
     */
    public static final String NACHNAME_PATTERN =
        "(o'|von|von der|von und zu|van)?[A-ZÄÖÜ][a-zäöüß]+(-[A-ZÄÖÜ][a-zäöüß]+)?";

    /**
     * Kleinster Wert für eine Kategorie.
     */
    public static final long MIN_KATEGORIE = 0L;

    /**
     * Maximaler Wert für eine Kategorie.
     */
    public static final long MAX_KATEGORIE = 9L;

    // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier
    // https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate
    // https://thorben-janssen.com/ultimate-guide-to-implementing-equals-and-hashcode-with-hibernate
    /**
     * Die ID des Kunden.
     * @param id Die ID.
     * @return Die ID.
     */
    @Id
    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#identifiers-generators-uuid
    // https://in.relation.to/2022/05/12/orm-uuid-mapping
    @GeneratedValue
    // Oracle: https://in.relation.to/2022/05/12/orm-uuid-mapping
    // @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @EqualsAndHashCode.Include
    private UUID id;

    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#locking-optimistic-mapping
    /**
     * Versionsnummer für optimistische Synchronisation.
     */
    @Version
    private int version;

    /**
     * Der Nachname des Kunden.
     * @param nachname Der Nachname.
     * @return Der Nachname.
     */
    @NotNull
    @Pattern(regexp = NACHNAME_PATTERN)
    @Size(max = 40)
    private String nachname;

    /**
     * Die Emailadresse des Kunden.
     * @param email Die Emailadresse.
     * @return Die Emailadresse.
     */
    @Email
    @NotNull
    @Size(max = 40)
    private String email;

    /**
     * Die Kategorie des Kunden.
     * @param kategorie Die Kategorie.
     * @return Die Kategorie.
     */
    @Min(MIN_KATEGORIE)
    @Max(MAX_KATEGORIE)
    private int kategorie;

    /**
     * Hat der Kunde den Newsletter abonniert.
     * @param hasNewsletter Ist der Newsletter abonniert?
     * @return Ist der Newsletter abonniert?
     */
    private boolean hasNewsletter;

    /**
     * Das Geburtsdatum des Kunden.
     * @param geburtsdatum Das Geburtsdatum.
     * @return Das Geburtsdatum.
     */
    @Past
    private LocalDate geburtsdatum;

    /**
     * Die URL zur Homepage des Kunden. Bei URI statt URL muss nur die Syntax stimmen und die Ressource muss nicht
     * existieren.
     * @param homepage Die URL zur Homepage.
     * @return Die URL zur Homepage.
     */
    @Column(length = 40)
    private URL homepage;

    /**
     * Das Geschlecht des Kunden.
     * @param geschlecht Das Geschlecht.
     * @return Das Geschlecht.
     */
    @Convert(converter = GeschlechtType.GeschlechtTypeConverter.class)
    private GeschlechtType geschlecht;

    /**
     * Der Familienstand des Kunden.
     * @param familienstand Der Familienstand.
     * @return Der Familienstand.
     */
    @Convert(converter = FamilienstandType.FamilienstandTypeConverter.class)
    private FamilienstandType familienstand;

    /**
     * Die Interessen des Kunden.
     * @param interessen Die Interessen.
     * @return Die Interessen.
     */
    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#access-embeddable-types
    // Fetch-Type EAGER bewirkt 2. SELECT-Query im Gegensatz zu Fetch-Join
    @ElementCollection
    @CollectionTable
    @Column(name = "interesse")
    @Convert(converter = InteresseType.InteresseTypeConverter.class)
    @UniqueElements
    @ToString.Exclude
    private List<InteresseType> interessen;

    /**
     * Der Umsatz des Kunden.
     * @param umsatz Der Umsatz.
     * @return Der Umsatz.
     */
    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#associations-one-to-one-unidirectional
    @OneToOne(cascade = {PERSIST, REMOVE}, fetch = LAZY)
    @JoinColumn(updatable = false)
    @ToString.Exclude
    private Umsatz umsatz;

    /**
     * Die Adresse des Kunden.
     * @param adresse Die Adresse.
     * @return Die Adresse.
     */
    @OneToOne(optional = false, cascade = {PERSIST, REMOVE}, fetch = LAZY)
    @JoinColumn(updatable = false)
    @Valid
    @ToString.Exclude
    private Adresse adresse;

    @Size(max = 20)
    private String username;

    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#mapping-generated-CreationTimestamp
    @CreationTimestamp
    private LocalDateTime erzeugt;

    // https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html...
    // ...#mapping-generated-UpdateTimestamp
    @UpdateTimestamp
    private LocalDateTime aktualisiert;

    /**
     * Kundendaten überschreiben.
     *
     * @param kunde Neue Kundendaten.
     */
    public void set(final Kunde kunde) {
        nachname = kunde.nachname;
        email = kunde.email;
        kategorie = kunde.kategorie;
        hasNewsletter = kunde.hasNewsletter;
        geburtsdatum = kunde.geburtsdatum;
        homepage = kunde.homepage;
        geschlecht = kunde.geschlecht;
        familienstand = kunde.familienstand;
    }
}
