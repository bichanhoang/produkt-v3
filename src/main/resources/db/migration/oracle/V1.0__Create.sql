-- Copyright (C) 2022 - present Juergen Zimmermann, Hochschule Karlsruhe
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see <https://www.gnu.org/licenses/>.

-- docker compose exec oracle sqlplus kunde/p@XEPDB1

-- Flyway unterstuetzt noch nicht Oracle 21 https://flywaydb.org/documentation/database/oracle
-- https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-TABLE.html
-- https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/Data-Types.html
CREATE TABLE login (
             -- https://docs.oracle.com/en/database/other-databases/nosql-database/21.1/sqlreferencefornosql/using-uuid-data-type.html
             -- impliziter Index fuer Primary Key
             -- RAW(16) fuer UUID wird von Vert.x bei Fremdschluessel nicht unterstuetzt; INSERT INTO waere bei UUID ohne "-"
    id       CHAR(36) PRIMARY KEY,
    username VARCHAR2(20) UNIQUE NOT NULL,
    password VARCHAR2(180) NOT NULL
);

CREATE TABLE login_rollen (
    login_id CHAR(36) NOT NULL REFERENCES login,
    rolle    VARCHAR2(20) NOT NULL CHECK (REGEXP_LIKE(rolle, 'ADMIN|KUNDE|ACTUATOR'))
);
ALTER TABLE login_rollen ADD CONSTRAINT login_rollen_pk PRIMARY KEY (login_id, rolle);

-- https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-INDEX.html
CREATE INDEX login_rollen_idx ON login_rollen(login_id);

CREATE TABLE umsatz (
    id        CHAR(36) PRIMARY KEY,
              -- 10 Stellen, davon 2 Nachkommastellen
    betrag    NUMBER(10,2) NOT NULL,
    waehrung  CHAR(3) NOT NULL CHECK (REGEXP_LIKE(waehrung, '[A-Z]{3}'))
);

CREATE TABLE adresse (
    id    CHAR(36) PRIMARY KEY,
    plz   CHAR(5) NOT NULL CHECK (REGEXP_LIKE(plz, '\d{5}')),
    ort   VARCHAR2(40) NOT NULL
);

CREATE INDEX adresse_plz_idx ON adresse(plz);

CREATE TABLE kunde (
    id            CHAR(36) PRIMARY KEY,
    version       NUMBER(10,0) NOT NULL,
    nachname      VARCHAR2(40) NOT NULL,
                  -- impliziter Index als B-Baum durch UNIQUE
    email         VARCHAR2(40) UNIQUE NOT NULL,
    kategorie     NUMBER(1,0) NOT NULL CHECK (kategorie >= 0 AND kategorie <= 9),
                  -- https://www.postgresql.org/docs/current/datatype-boolean.html
    has_newsletter NUMBER(1,0) NOT NULL CHECK (has_newsletter = 0 OR has_newsletter = 1),
    geburtsdatum  DATE,
    homepage      VARCHAR2(40),
    geschlecht    CHAR(1) CHECK (REGEXP_LIKE(geschlecht, 'M|W|D')),
    familienstand VARCHAR2(2) CHECK (REGEXP_LIKE(familienstand, 'L|VH|G|VW')),
    umsatz_id     CHAR(36) REFERENCES umsatz,
    adresse_id    CHAR(36) NOT NULL REFERENCES adresse,
    username      VARCHAR2(20) NOT NULL REFERENCES login(username),
    erzeugt       TIMESTAMP NOT NULL,
    aktualisiert  TIMESTAMP NOT NULL
);

CREATE INDEX kunde_nachname_idx ON kunde(nachname);

CREATE TABLE kunde_interessen (
    kunde_id  CHAR(36) NOT NULL REFERENCES kunde,
    interesse CHAR(1) NOT NULL CHECK (REGEXP_LIKE(interesse, 'S|L|R'))
);
ALTER TABLE kunde_interessen ADD CONSTRAINT kunde_interessen_pk PRIMARY KEY (kunde_id, interesse);

CREATE INDEX kunde_interessen_kunde_idx ON kunde_interessen(kunde_id);
