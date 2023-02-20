/*
Copyright 2020, 2021, 2023 WeAreFrank!

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
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import nl.nn.adapterframework.jdbc.JdbcException;

/**
* Support for MariaDB.
*
*/
public class MariaDbDbmsSupport extends MySqlDbmsSupport {

	private Boolean dbmsHasSkipLockedFunctionality;
	private String productVersion;

	public MariaDbDbmsSupport() {
		this(false);
	}

	public MariaDbDbmsSupport(String productVersion) {
		this.productVersion = productVersion;
	}

	public MariaDbDbmsSupport(boolean dbmsHasSkipLockedFunctionality) {
		this.dbmsHasSkipLockedFunctionality = dbmsHasSkipLockedFunctionality;
	}


	@Override
	public Dbms getDbms() {
		return Dbms.MARIADB;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		if (dbmsHasSkipLockedFunctionality==null) {
			if (StringUtils.isNotEmpty(productVersion)) {
				String strippedProductVersion = productVersion;
				int colonPos = productVersion.indexOf(":");
				if (colonPos>=0) {
					String secondPart=productVersion.substring(colonPos+1);
					int plusPos=secondPart.indexOf("+");
					if (plusPos>0) {
						strippedProductVersion=secondPart.substring(0, plusPos);
					}
					log.debug("stripped productversion [{}] down to [{}]", productVersion, strippedProductVersion);
				}
				DefaultArtifactVersion thisVersion = new DefaultArtifactVersion(strippedProductVersion);
				DefaultArtifactVersion targetVersion = new DefaultArtifactVersion("10.6.0");
				dbmsHasSkipLockedFunctionality = thisVersion.compareTo(targetVersion) >= 0;
				log.debug("based on Mariadb productversion [{}] dbms hasSkipLockedFunctionality [{}]", strippedProductVersion, dbmsHasSkipLockedFunctionality);
			} else {
				dbmsHasSkipLockedFunctionality = false; // to be safe
			}
		}
		return dbmsHasSkipLockedFunctionality;
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE "+ (hasSkipLockedFunctionality() ? "SKIP LOCKED" : "WAIT 1"); // Mariadb used to have no 'skip locked', WAIT 1 is next best
		}
		return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE WAIT "+wait;
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:""); // Mariadb has no 'skip locked'
		}
		throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
	}

	@Override
	public Object getClobHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return rs.getStatement().getConnection().createClob();
	}

	@Override
	public Object getClobHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return rs.getStatement().getConnection().createClob();
	}

	@Override
	public Object getBlobHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return rs.getStatement().getConnection().createBlob();
	}
	@Override
	public Object getBlobHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return rs.getStatement().getConnection().createBlob();
	}

}
