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
package nl.nn.adapterframework.jta.narayana;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory {
	private NarayanaRecoveryManager recoveryManager;

	@Override
	protected DataSource augment(CommonDataSource dataSource, String dataSourceName) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper((XADataSource) dataSource);
		this.recoveryManager.registerXAResourceRecoveryHelper(recoveryHelper);

		return new NarayanaDataSource((DataSource) dataSource);
	}

	public void setRecoveryManager(NarayanaRecoveryManager recoveryManager) {
		this.recoveryManager = recoveryManager;
	}
}
