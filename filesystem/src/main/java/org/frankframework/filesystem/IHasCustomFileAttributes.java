package org.frankframework.filesystem;

import jakarta.annotation.Nonnull;
import org.frankframework.parameters.ParameterValueList;

public interface IHasCustomFileAttributes<F> {
	String FILE_ATTRIBUTE_PARAM_PREFIX = "FileAttribute.";

	void setCustomFileAttribute(@Nonnull F file, @Nonnull String key, @Nonnull String value);

	default void setCustomFileAttributes(@Nonnull F file, @Nonnull ParameterValueList pvl) {
		pvl.stream()
				.filter(pv -> pv.getName().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.forEach(pv -> setCustomFileAttribute(file, pv.getName().substring(FILE_ATTRIBUTE_PARAM_PREFIX.length()), pv.asStringValue()));
	}
}
