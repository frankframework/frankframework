/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.jta;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionException;

public class ThreadConnectableDataSourceTransactionManager extends DataSourceTransactionManager implements IThreadConnectableTransactionManager<Object,Object> {

	public ThreadConnectableDataSourceTransactionManager() {
		super();
	}

	public ThreadConnectableDataSourceTransactionManager(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Object getCurrentTransaction() throws TransactionException {
		return doGetTransaction();
	}

	@Override
	public SuspendedResourcesHolder suspendTransaction(Object transaction) {
		return suspend(transaction);
	}

	@Override
	public void resumeTransaction(Object transaction, Object resources) {
		resume(transaction, (SuspendedResourcesHolder)resources);
	}

}
