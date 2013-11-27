/*
 * The Fascinator - Generic User
 * Copyright (C) 2008-2011 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.googlecode.fascinator.common.authentication;

/**
 * A basic user object, does not define its metadata schema, that is left to
 * extending classes, but creates access methods against an unknown schema.
 * 
 * @author Andrew Brazzatti
 */
public class SpringUser extends GenericUser {
    private boolean ssoRolesSet;

    public boolean isSsoRolesSet() {
        return ssoRolesSet;
    }

    public void setSsoRolesSet(boolean ssoRolesSet) {
        this.ssoRolesSet = ssoRolesSet;
    }

}
