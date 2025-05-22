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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;

/**
 * An interface indicating that a filesystem supports adding custom (user-defined) file attributes. Attributes
 * are always set as key-value pairs. The values are always strings.
 *<p>
 *     A {@link IBasicFileSystem} that implements this interface should return any user-defined metadata attributes
 *     in the method {@link IBasicFileSystem#getAdditionalFileProperties(Object)}.
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
	 * Method to be used in configure.
	 * @param pvl the ParameterValueList, may be {@code null}.
	 * @return true when at least one {@link IParameter Parameter} name starts with {@value #FILE_ATTRIBUTE_PARAM_PREFIX}.
	 */
	default boolean hasCustomFileAttributes(@Nullable ParameterList pvl) {
		if (pvl == null) {
			return false;
		}

		return pvl.stream()
				.filter(pv -> pv.getName().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.count() > 0;
	}

	/**
	 * Gets the custom attributes from the {@link ParameterValueList}. The parameter value list
	 * is filtered on parameter names with the prefix {@link #FILE_ATTRIBUTE_PARAM_PREFIX} ({@value #FILE_ATTRIBUTE_PARAM_PREFIX}).
	 * <p>
	 *     For each parameter-value, if the parameter-name starts with the prefix {@value #FILE_ATTRIBUTE_PARAM_PREFIX} the
	 *     prefix is stripped from the name to get the attribute-name and the string-value of the parameter is
	 *     used to get the attribute-value.
	 * </p>
	 * <p>
	 *     This is an interface default method that most implementations should not need to override.
	 * </p>
	 *
	 * @param pvl {@link ParameterValueList} containing inputs to apply as user-defined file metadata
	 * @return a Map with the custom file attributes
	 */
	default Map<String, String> getCustomFileAttributes(@Nonnull ParameterValueList pvl) {
		return pvl.stream()
				.filter(pv -> pv.getName().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.collect(Collectors.toMap(parameterValue -> parameterValue.getName().substring(FILE_ATTRIBUTE_PARAM_PREFIX.length()), ParameterValue::asStringValue));
	}

	/**
	 * Creates a file with the given custom file attributes.
	 *
	 * @param file
	 * @param contents
	 * @param customFileAttributes
	 */
	void createFile(F file, InputStream contents, Map<String, String> customFileAttributes) throws FileSystemException, IOException;

	/**
	 * Sets the given custom file attribute on the given file. If the attribute already exists, it will be overwritten.
	 * If the filesystem cannot support this operation but can support other operations in this interface, it should silently ignore this call and
	 * not throw an exception.
	 *
	 * @param file File object for which to set the extended attribute. Must not be {@code null}.
	 * @param name Name of the extended attribute to set. Must not be {@code null}.
	 * @param value Value of the extended attribute to set. Must not be {@code null}.
	 */
	void setCustomFileAttribute(@Nonnull F file, @Nonnull String name, @Nonnull String value) throws FileSystemException;

	/**
	 * Gets the value of the given custom file attribute on the given file. If the attribute does not exist, {@code null} is returned.
	 *
	 * @param file File object for which to get the extended attribute. Must not be {@code null}.
	 * @param name Name of the extended attribute to get. Must not be {@code null}.
	 * @return Value of the extended attribute or {@code null} if the attribute does not exist.
	 */
	@Nullable String getCustomFileAttribute(@Nonnull F file, @Nonnull String name) throws FileSystemException;
}
