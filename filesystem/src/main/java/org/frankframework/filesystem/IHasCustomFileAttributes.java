package org.frankframework.filesystem;

import java.util.Map;

import jakarta.annotation.Nonnull;

public interface IHasCustomFileAttributes<F> {
	String FILE_ATTRIBUTE_PARAM_PREFIX = "FileAttribute.";

	void setCustomFileAttribute(@Nonnull F file, @Nonnull String key, @Nonnull String value);

	default void setCustomProperties(@Nonnull F file, @Nonnull Map<String, String> customFileAttributes) {
		customFileAttributes.entrySet()
				.stream()
				.filter(entry -> entry.getKey().startsWith(FILE_ATTRIBUTE_PARAM_PREFIX))
				.forEach(entry -> setCustomFileAttribute(file, entry.getKey().substring(FILE_ATTRIBUTE_PARAM_PREFIX.length()), entry.getValue()));
	}
}
