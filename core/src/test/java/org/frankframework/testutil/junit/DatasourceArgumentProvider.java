package org.frankframework.testutil.junit;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.util.AnnotationUtils;

import org.frankframework.testutil.TransactionManagerType;

/**
 * Provides the database matrix as Test Arguments
 *
 * @author Niels Meijer
 */
public class DatasourceArgumentProvider implements ArgumentsProvider {

	@NonNull
	@Override
	public final Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		AnnotatedElement testMethod = context.getTestMethod().orElseGet(context::getRequiredTestMethod);
		Optional<DatabaseTestOptions> annotation = AnnotationUtils.findAnnotation(testMethod, DatabaseTestOptions.class);

		TransactionManagerType type = getTransactionManagerType();
		List<String> availableDataSources = type.getAvailableDataSources();
		List<String> customDataSources = annotation
				.map(DatabaseTestOptions::additionalDataSources)
				.stream()
				.flatMap(Arrays::stream)
				.filter(StringUtils::isNotBlank)
				.toList();

		return Stream.concat(availableDataSources.stream(), customDataSources.stream())
				.map(dsName -> Arguments.of(type, dsName));
	}

	protected TransactionManagerType getTransactionManagerType() {
		return TransactionManagerType.DATASOURCE;
	}
}
