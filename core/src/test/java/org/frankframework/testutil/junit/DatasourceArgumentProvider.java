package org.frankframework.testutil.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	@Override
	public final Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
		if(context.getTestClass().isEmpty()) {
			throw new IllegalStateException("test class not found");
		}

		List<Arguments> args = new ArrayList<>();
		TransactionManagerType type = getTransactionManagerType();
		List<String> availableDataSources = type.getAvailableDataSources();
		args.addAll(availableDataSources.stream().map(dsName -> Arguments.of(type, dsName)).collect(Collectors.toList()));
		return args.stream();
	}

	protected TransactionManagerType getTransactionManagerType() {
		return TransactionManagerType.DATASOURCE;
	}
}
