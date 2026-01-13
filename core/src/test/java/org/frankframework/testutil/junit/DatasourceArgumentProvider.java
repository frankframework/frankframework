package org.frankframework.testutil.junit;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

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
		if(context.getTestClass().isEmpty()) {
			throw new IllegalStateException("test class not found");
		}

		TransactionManagerType type = getTransactionManagerType();
		List<String> availableDataSources = type.getAvailableDataSources();
		return availableDataSources.stream().map(dsName -> Arguments.of(type, dsName));
	}

	protected TransactionManagerType getTransactionManagerType() {
		return TransactionManagerType.DATASOURCE;
	}
}
