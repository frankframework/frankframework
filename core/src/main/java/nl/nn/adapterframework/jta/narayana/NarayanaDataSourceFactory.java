/*
   Copyright 2021-2023 WeAreFrank!

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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.logging.log4j.Logger;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.IsSameRMModifier;
import com.arjuna.ats.internal.jdbc.drivers.modifiers.ModifierFactory;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.LogUtil;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory {
	protected static Logger log = LogUtil.getLogger(NarayanaDataSourceFactory.class);

	private @Getter @Setter int maxPoolSize=20;

	private @Setter NarayanaJtaTransactionManager transactionManager;

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if (dataSource instanceof XADataSource) {
			XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper((XADataSource) dataSource);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);
			NarayanaDataSource result = new NarayanaDataSource(dataSource, dataSourceName);
			result.setMaxConnections(maxPoolSize);
			checkModifiers(result);
			return result;
		}
		log.warn("DataSource [{}] is not XA enabled", dataSourceName);
		return (DataSource) dataSource;
	}

	public static void checkModifiers(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			String driverName = metadata.getDriverName();
			int major = metadata.getDriverMajorVersion();
			int minor = metadata.getDriverMinorVersion();

			if (ModifierFactory.getModifier(driverName, major, minor)==null) {
				log.info("No Modifier found for driver [{}] version [{}.{}], creating IsSameRM modifier", driverName, major, minor);
				ModifierFactory.putModifier(driverName, major, minor, IsSameRMModifier.class.getName());
			}
		} catch (SQLException e) {
			log.warn("Could not check for existence of Modifier", e);
		}
	}

}
