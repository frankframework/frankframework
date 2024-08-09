package org.frankframework.web;

import jakarta.annotation.security.RolesAllowed;

/**
 * To avoid repeating this list of roles over and over again, use a default annotation
 */
public @interface AllRolesAllowed {
	RolesAllowed value() default @RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"});
}
