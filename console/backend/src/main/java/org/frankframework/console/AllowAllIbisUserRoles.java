/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.console;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.frankframework.lifecycle.DynamicRegistration;

import jakarta.annotation.security.RolesAllowed;

/**
 * To avoid repeating this list of roles over and over again, use a default annotation
 * Since you can't reference a static list of values, it has to be hard coded like this.
 *
 * @see DynamicRegistration#ALL_IBIS_USER_ROLES
 */
@Documented
@Retention (RUNTIME)
@Target(METHOD)
@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
public @interface AllowAllIbisUserRoles {
}
