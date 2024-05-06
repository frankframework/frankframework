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
package org.frankframework.ibistesttool.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;

import lombok.Setter;

public class OptionalJtaTransactionManager implements PlatformTransactionManager, InitializingBean {
	private JtaTransactionManager jtaTransactionManager;
	private DataSourceTransactionManager dataSourceTransactionManager;
	private @Setter DataSource dataSource;

	OptionalJtaTransactionManager() {
		jtaTransactionManager = new JtaTransactionManager();
	}

	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		try {
			jtaTransactionManager.afterPropertiesSet();
		} catch(IllegalStateException e) {
			dataSourceTransactionManager = new DataSourceTransactionManager();
			dataSourceTransactionManager.setDataSource(dataSource);
			dataSourceTransactionManager.afterPropertiesSet();
		}
	}

	@Override
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			return dataSourceTransactionManager.getTransaction(definition);
		} else {
			return jtaTransactionManager.getTransaction(definition);
		}
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			dataSourceTransactionManager.commit(status);
		} else {
			jtaTransactionManager.commit(status);
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		if (dataSourceTransactionManager != null) {
			dataSourceTransactionManager.rollback(status);
		} else {
			jtaTransactionManager.rollback(status);
		}
	}
}
