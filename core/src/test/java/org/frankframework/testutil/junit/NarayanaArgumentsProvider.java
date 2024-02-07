package org.frankframework.testutil.junit;

import org.frankframework.testutil.TransactionManagerType;

/**
 * Provides the database matrix as Test Arguments
 *
 * @author Niels Meijer
 */
public class NarayanaArgumentsProvider extends DatasourceArgumentSource {

	@Override
	protected TransactionManagerType getTransactionManagerType() {
		return TransactionManagerType.NARAYANA;
	}
}
