/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2023, 2025-2026 WeAreFrank!

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
package org.frankframework.scheduler;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Extending the Spring SchedulerFactoryBean because it starts the Quartz scheduler instance.
 * This instance can get detached from the ApplicationContext when it fails to execute the refresh method.
 * The issue arose when the IBIS is reconnecting but fails to init the txManager.
 *
 * @author	Niels Meijer
 *
 */
public class SchedulerFactoryBean extends org.springframework.scheduling.quartz.SchedulerFactoryBean {

	@Override
	public void setDataSource(@NonNull DataSource dataSource) {
		// Make sure this isn't autowired by Spring
	}
	@Override
	public void setTransactionManager(@NonNull PlatformTransactionManager transactionManager) {
		// Make sure this isn't autowired by Spring
	}
}
