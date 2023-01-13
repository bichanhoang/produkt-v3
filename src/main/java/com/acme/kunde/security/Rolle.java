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
package com.acme.kunde.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Singleton für verfügbare Rollen als Strings für das Spring-Interface GrantedAuthority.
 *
 * @author <a href="mailto:Juergen.Zimmermann@h-ka.de">Jürgen Zimmermann</a>
 */
@SuppressWarnings("UtilityClassCanBeEnum")
public final class Rolle {
    /**
     * Die Rolle ADMIN.
     */
    public static final String ADMIN = "ADMIN";

    /**
     * Die Rolle KUNDE.
     */
    public static final String KUNDE = "KUNDE";

    /**
     * Die Rolle ACTUATOR.
     */
    public static final String ACTUATOR = "ACTUATOR";

    /**
     * Präfix für die Rollennamen gemäß Spring Security.
     */
    static final String ROLE_PREFIX = "ROLE_";

    /**
     * Die Rolle ADMIN mit Präfix ROLE_ für Spring Security.
     */
    public static final String ADMIN_STR = ROLE_PREFIX + ADMIN;

    /**
     * Die Rolle KUNDE mit Präfix ROLE_ für Spring Security.
     */
    private static final String KUNDE_STR = ROLE_PREFIX + KUNDE;

    /**
     * Die Rolle KUNDE als GrantedAuthority für Spring Security.
     */
    static final GrantedAuthority KUNDE_AUTHORITY = new SimpleGrantedAuthority(KUNDE_STR);

    private Rolle() {
    }
}
