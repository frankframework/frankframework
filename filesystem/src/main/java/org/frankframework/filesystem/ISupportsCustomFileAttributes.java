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

public interface ISupportsCustomFileAttributes<F> {
	String FILE_ATTRIBUTE_PARAM_PREFIX = "FileAttribute.";

	void setCustomFileAttribute(@Nonnull F file, @Nonnull String key, @Nonnull String value);

	default void setCustomFileAttributes(@Nonnull F file, @Nonnull ParameterValueList pvl) {
		pvl.stream()
				.filter(pv -> pv.getName().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.forEach(pv -> setCustomFileAttribute(file, pv.getName().substring(FILE_ATTRIBUTE_PARAM_PREFIX.length()), pv.asStringValue()));
	}
}
