/*
Copyright 2017 Integration Partners B.V.

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
package nl.nn.adapterframework.http.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.util.LogUtil;

/**
 * This class refers to rules that determine who is allowed to do what.
 * @author Niels Meijer
 *
 */

public class ApiAuthorization {

	private FixedQuerySender querySender = null;
	private final String query = "SELECT A.C, A.R, A.U, A.D FROM IBISAUTHENTICATION A, IBISUSERS_GROUPS UG WHERE A.URIPATTERN=? AND (A.USER_ID=? OR (A.GROUP_ID=UG.GROUP_ID AND UG.USER_ID=?))";
	private Logger log = LogUtil.getLogger(this);
	private static ApiAuthorization self = null;

	public static ApiAuthorization getInstance(IbisContext ibisContext, String jmsRealm) throws ConfigurationException {
		if( self == null ) {
			self = new ApiAuthorization(ibisContext, jmsRealm);
		}
		return self;
	}

	ApiAuthorization(IbisContext ibisContext, String jmsRealm) throws ConfigurationException {
		querySender = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		querySender.setJmsRealm(jmsRealm);
		querySender.setQuery("SELECT COUNT(*) FROM IBISAUTH");
		querySender.configure();
	}

	public Map<String, Boolean> getAllowedMethods(String uriPattern, ApiPrincipal userPrincipal) {
		// TODO Auto-generated method stub
		HashMap<String, Boolean> bar = new HashMap<String, Boolean>();
		bar.put("POST", true);
		bar.put("GET", true);
		bar.put("PUT", true);
		bar.put("PATCH", true);
		bar.put("DELETE", true);

		bar.put("OPTIONS", true);
		return bar;
	}

	public Map<String, Boolean> getAllowedMethods2(String uriPattern, ApiPrincipal principal) throws SenderException {
		Map<String, Boolean> CRUD = null;
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = querySender.getConnection();
			querySender.open();
			conn = querySender.getConnection();
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, principal.getID());
			stmt.setString(2, uriPattern);
			ResultSet rs = stmt.executeQuery();
			CRUD = new HashMap<String, Boolean>(4);
			if (rs.next()) {
				CRUD.put("C", rs.getBoolean(1));
				CRUD.put("R", rs.getBoolean(2));
				CRUD.put("U", rs.getBoolean(3));
				CRUD.put("D", rs.getBoolean(4));
			}
		}
		catch (Exception e) {
			throw new SenderException(e);
		}
		finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					log.warn("Could not close statement", e);
				}
			}
			querySender.close();
		}

		return CRUD;
	}
}
