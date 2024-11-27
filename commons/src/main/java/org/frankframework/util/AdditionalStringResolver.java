/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.util;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface to be implemented by framework-classes that provide extra property-resolving sources, such as credentials, which cannot be directly included of the
 * {@link StringResolver} methods.
 * <p>
 * Implementors of the interface should have a no-args constructor and not need any injected dependencies. The {@link StringResolver} will create instances
 * of each class that implements this interface, and invoke it from {@link StringResolver#substVars(String, Map, Map, Set, String, String, boolean)}.
 * </p>
 * <p>
 * Implementors are loaded via the JDK {@link java.util.ServiceLoader} mechanism.
 * </p>
 * <p>
 * It is <b>not</b> allowed to use a static reference to the logger in implementations.
 * Log4j2 uses StringResolver during instantiation.
 * </p>
 */
public interface AdditionalStringResolver {

	/**
	 * Method to implement string resolution.
	 * <p>
	 * Parameters are mostly as from {@link StringResolver#substVars(String, Map, Map, Set, String, String, boolean)}, except the first parameter,
	 * {@code key}, which is the key to be resolved instead of the full string in which to substitute.
	 * </p>
	 *
	 * @param key                     Key to look up
	 * @param props1                  First property map in which to look up values
	 * @param props2                  Second property map in which to look up values
	 * @param propsToHide             List of properties to hide. If {@code null}, then no hiding of properties should be done.
	 *                                If not {@code null}, any properties whose name is in the collection will be hidden by
	 *                                the caller but the implementation may make its own decision on hiding property values.
	 *                                For instance, hiding credentials.
	 * @param delimStart              Start delimiter, normally only needed by caller
	 * @param delimStop               End delimiter, normally only needed by caller
	 * @param resolveWithPropertyName Flag if values should be prefixed with name of resolved property, normally only
	 *                                needed by caller.
	 * @return Resolved property value, or {@link Optional#empty()} if it cannot be resolved by this implementation. If {@link Optional#empty()} is
	 * 		returned, the {@link StringResolver} will then continue to try resolving the {@code key}. If any non-empty {@link Optional} is returned,
	 * 		the {@link StringResolver} will use the value of that as value for the {@code key} and not look for other resolutions for the key.
	 */
	Optional<String> resolve(String key, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName);
}
