/*
   Copyright 2023 WeAreFrank!

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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreAPI;
import com.arjuna.ats.arjuna.objectstore.StateStatus;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCImple_driver;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * See {@link JDBCStore} for more info and references.
 
 * @author Niels Meijer
 *
 */
@Log4j2
public class JndiObjectStore implements ObjectStoreAPI {
	private static final String DEFAULT_TABLE_NAME = "JBossTS_TX_";
	private JDBCImple_driver driverImplementation;
	private static Map<String, JDBCImple_driver> imples = new HashMap<>();
	private final String tableName;
	private final String storeName;

	public JndiObjectStore(final ObjectStoreEnvironmentBean jdbcStoreEnvironment) {
		throw new IllegalStateException("JndiObjectStore should not be instantiated via Narayana");
	}

	public JndiObjectStore(final DataSource datasource, final String tableSuffix, final ObjectStoreEnvironmentBean jdbcStoreEnvironment) throws ObjectStoreException {
		tableName = DEFAULT_TABLE_NAME + tableSuffix;
		storeName = "JndiDataSourceJDBCAccess:" + tableName;

		driverImplementation = imples.get(storeName);
		if (driverImplementation == null) {
			log.info("creating new JDBCImple_driver for JDBCStore [{}] with DataSource [{}]", storeName, datasource);
			JDBCAccess jdbcAccess = new JndiDataSourceJDBCAccess(datasource);
			driverImplementation = createJdbcDriver(jdbcAccess);
			try {
				driverImplementation.initialise(jdbcAccess, tableName, jdbcStoreEnvironment);
			} catch (SQLException | NamingException e) {
				throw new ObjectStoreException("unable configure JndiObjectStore", e);
			}
			log.info("created JDBCImple driver for JDBCStore [{}]", storeName);
			imples.put(storeName, driverImplementation);
		}
	}

	private JDBCImple_driver createJdbcDriver(JDBCAccess jdbcAccess) throws ObjectStoreException {
		try {
			Connection connection = jdbcAccess.getConnection();
			try {
				DatabaseMetaData md = connection.getMetaData();
				return findDriverClass(md.getDriverName());
			} finally {
				connection.close();
			}
		} catch (ReflectiveOperationException e) {
			log.fatal("unable to create JDBCImple_driver for JDBCStore [{}]", storeName, e);
			throw new ObjectStoreException("unable to create JDBCImple_driver class", e);
		} catch (SQLException e) {
			log.fatal("unable to create JDBCImple_driver for JDBCStore [{}] using DataSource [{}]", storeName, jdbcAccess, e);
			throw new ObjectStoreException("unable to create JDBCImple_driver using datasource ["+jdbcAccess+"]", e);
		}
	}

	private JDBCImple_driver findDriverClass(final String driverName) throws ReflectiveOperationException {
		if(StringUtils.isBlank(driverName)) {
			throw new IllegalArgumentException("driverName may not be empty");
		}

		final String packagePrefix = JDBCStore.class.getName().substring(0, JDBCStore.class.getName().lastIndexOf('.')) + ".drivers.";
		String driverToLoad = packagePrefix + sanitizeDriverName(driverName) + "_driver";
		log.info("attempting to load JDBCImple driver class [{}]", driverToLoad);

		Class<?> clazz = ClassUtils.loadClass(driverToLoad);
		if(!JDBCImple_driver.class.isAssignableFrom(clazz)) {
			throw new ReflectiveOperationException("driver class is not instance of JDBCImple");
		}

		Constructor<?> con = ClassUtils.getConstructorOnType(clazz, new Class[] {});
		return (JDBCImple_driver) con.newInstance();
	}

	/**
	 *  Check for spaces in the name.
	 *  Narayana implementation classes are always just the first part of such names.
	 *  Replace hyphens with underscores and make sure the drivername is all lowercase.
	 *  See all implementations of {@link JDBCImple_driver}.
	 */
	private String sanitizeDriverName(String driverName) {
		int index = driverName.indexOf(' ');
		if(index != -1) {
			driverName = driverName.substring(0, index);
		}
		return driverName.replace('-', '_').toLowerCase();
	}

	@Override
	public void start() {
		// NO-OP
	}

	@Override
	public void stop() {
		// NO-OP
	}

	/**
	 * Implement full write_uncommitted/commit protocol
	 */
	@Override
	public boolean fullCommitNeeded() {
		return true;
	}

	/**
	 * Nothing needs to be flushed, it's always directly stored.
	 */
	@Override
	public void sync() {
		//Nothign to sync or flush
	}

	@Override
	public boolean isType(Uid u, String tn, int st) throws ObjectStoreException {
		return (currentState(u, tn) == st);
	}

	@Override
	public String getStoreName() {
		return storeName;
	}

	@Override
	public boolean allObjUids(String s, InputObjectState buff) throws ObjectStoreException {
		System.err.println("allObjUids (bool): "+s);
		return allObjUids(s, buff, StateStatus.OS_UNKNOWN);
	}

	@Override
	public boolean commit_state(Uid objUid, String tName) throws ObjectStoreException {
		System.err.println("commit_state: "+objUid+" - "+tName);
		return driverImplementation.commit_state(objUid, tName);
	}

	@Override
	public boolean hide_state(Uid objUid, String tName) throws ObjectStoreException {
		System.err.println("hide_state: "+objUid+" - "+tName);
		return driverImplementation.hide_state(objUid, tName);
	}

	@Override
	public boolean reveal_state(Uid objUid, String tName) throws ObjectStoreException {
		System.err.println("reveal_state: "+objUid+" - "+tName);
		return driverImplementation.reveal_state(objUid, tName);
	}

	@Override
	public int currentState(Uid objUid, String tName) throws ObjectStoreException {
		System.err.println("currentState: "+objUid+" - "+tName);
		return driverImplementation.currentState(objUid, tName);
	}

	@Override
	public InputObjectState read_committed(Uid storeUid, String tName) throws ObjectStoreException {
		System.err.println("read_committed: "+storeUid+" - "+tName);
		return driverImplementation.read_state(storeUid, tName, StateStatus.OS_COMMITTED);
	}

	@Override
	public InputObjectState read_uncommitted(Uid storeUid, String tName) throws ObjectStoreException {
		System.err.println("read_uncommitted: "+storeUid+" - "+tName);
		return driverImplementation.read_state(storeUid, tName, StateStatus.OS_UNCOMMITTED);
	}

	@Override
	public boolean remove_committed(Uid storeUid, String tName) throws ObjectStoreException {
		System.err.println("remove_committed: "+storeUid+" - "+tName);
		return driverImplementation.remove_state(storeUid, tName, StateStatus.OS_COMMITTED);
	}

	@Override
	public boolean remove_uncommitted(Uid storeUid, String tName) throws ObjectStoreException {
		System.err.println("remove_uncommitted: "+storeUid+" - "+tName);
		return driverImplementation.remove_state(storeUid, tName, StateStatus.OS_UNCOMMITTED);
	}

	@Override
	public boolean write_committed(Uid storeUid, String tName, OutputObjectState state) throws ObjectStoreException {
		System.err.println("write_committed: "+storeUid+" - "+tName +" - "+ state.toString());
		return driverImplementation.write_state(storeUid, tName, state, StateStatus.OS_COMMITTED);
	}

	@Override
	public boolean write_uncommitted(Uid storeUid, String tName, OutputObjectState state) throws ObjectStoreException {
		System.err.println("write_uncommitted: "+storeUid+" - "+tName);
		return driverImplementation.write_state(storeUid, tName, state, StateStatus.OS_UNCOMMITTED);
	}

	@Override
	public boolean allObjUids(String tName, InputObjectState state, int match) throws ObjectStoreException {
		System.err.println("allObjUids: "+tName);
		return driverImplementation.allObjUids(tName, state, match);
	}

	@Override
	public boolean allTypes(InputObjectState foundTypes) throws ObjectStoreException {
		return driverImplementation.allTypes(foundTypes);
	}
}