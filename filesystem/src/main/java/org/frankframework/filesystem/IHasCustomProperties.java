package org.frankframework.filesystem;

import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;

public interface IHasCustomProperties<F> {
	@Nonnull
	Set<String> getCustomPropertyNames();

	void setCustomPropertyNames(@Nonnull Set<String> customPropertyNames);

	@Nonnull
	Map<String, String> getCustomProperties(@Nonnull F file);

	void setCustomProperty(@Nonnull F file, @Nonnull String key, @Nonnull String value);

	default void setCustomProperties(@Nonnull F file, @Nonnull Map<String, String> customProperties) {
		customProperties.entrySet()
				.stream()
				.filter(entry -> getCustomPropertyNames().contains(entry.getKey()))
				.forEach(entry -> setCustomProperty(file, entry.getKey(), entry.getValue()));
	}
}
