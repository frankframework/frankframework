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
package org.frankframework.filesystem;

import jakarta.annotation.Nonnull;
import org.frankframework.parameters.ParameterValueList;

/**
 * An interface indicating that a filesystem supports adding custom (user-defined) file attributes. Attributes
 * are always set as key-value pairs. The values are always strings.
 *<p>
 *     A filesystem that implements this interface should return any user-defined metadata attributes
 *     in the method {@link IBasicFileSystem#getAdditionalFileProperties(F)}.
 *</p>
 *
 * @param <F> The type of the file used by the implementation. See also {@link IBasicFileSystem}.
 */
public interface ISupportsCustomFileAttributes<F> {
	/**
	 * Prefix for parameter-names, for parameters that should be set as custom metadata attributes.
	 * The prefix will not be part of the actual metadata attribute name set on the file, it is only
	 * used for filtering parameters to use for the custom metadata and stripped from the name when applying
	 * the attributes to the file.
	 */
	String FILE_ATTRIBUTE_PARAM_PREFIX = "FileAttribute.";

	/**
	 * Set a custom attributes on the given file.
	 * <p>
	 *     <em>NOTE:</em>
	 *     At the time this method is called, the file is not yet guaranteed to actually exist
	 *     in the underlying filesystem.
	 * </p>
	 *
	 * @param file File on which to set the attribute
	 * @param key Name of the attribute
	 * @param value Value of the attribute
	 */
	void setCustomFileAttribute(@Nonnull F file, @Nonnull String key, @Nonnull String value);

	/**
	 * Set custom attributes from the {@link ParameterValueList}. The parameter value list
	 * is filtered on parameter names with the prefix {@link #FILE_ATTRIBUTE_PARAM_PREFIX} ({@code "FileAttribute."}).
	 * <p>
	 *     For each parameter-value, if the parameter-name starts with the prefix {@code "FileAttribute."} the
	 *     prefix is stripped from the name to get the attribute-name and the string-value of the parameter is
	 *     used to get the attribute-value.
	 * </p>
	 * <p>
	 *     This is an interface default method that most implementations should not need to override.
	 * </p>
	 * @param file File on which to set the custom attributes
	 * @param pvl {@link ParameterValueList} containing inputs
	 */
	default void setCustomFileAttributes(@Nonnull F file, @Nonnull ParameterValueList pvl) {
		pvl.stream()
				.filter(pv -> pv.getName().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.forEach(pv -> setCustomFileAttribute(file, pv.getName().substring(FILE_ATTRIBUTE_PARAM_PREFIX.length()), pv.asStringValue()));
	}
}
