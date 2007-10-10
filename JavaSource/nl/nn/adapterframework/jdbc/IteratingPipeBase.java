/*
 * $Log: IteratingPipeBase.java,v $
 * Revision 1.2.2.1  2007-10-10 14:30:47  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.3  2007/10/08 12:18:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.2  2007/07/26 16:14:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check for null resultset
 *
 * Revision 1.1  2007/07/17 11:16:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added iterating classes
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.IteratingPipe;


/**
 * Base class for JDBC iterating pipes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class IteratingPipeBase extends IteratingPipe {
	
	protected FixedQuerySender querySender = new FixedQuerySender();

	public void configure() throws ConfigurationException {
		super.configure();
		querySender.setName("source of "+getName());
		querySender.configure();
	}
	
	public void start() throws PipeStartException {
		try {
			querySender.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	public void stop() {
		super.stop();
		try {
			querySender.close();
		} catch (SenderException e) {
			log.warn("Exception closing sender",e);
		}
	}

	protected abstract IDataIterator getIterator(ResultSet rs) throws SenderException; 

	protected IDataIterator getIterator(Object input, PipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		Connection connection = null;
		try {
			connection = querySender.getConnection();
			PreparedStatement statement=null;
//			try {
				String msg = (String)input;
				statement = querySender.getStatement(connection, correlationID, msg);
				ParameterResolutionContext prc = new ParameterResolutionContext(msg,session);
				if (prc != null && querySender.paramList != null) {
					querySender.applyParameters(statement, prc.getValues(querySender.paramList));
				}
				ResultSet rs = statement.executeQuery();
				if (rs==null || !rs.next()) {
					throw new SenderException("query has empty resultset");
				}
				return getIterator(rs);
//			} finally {
//				try {
//					if (statement!=null) {
//						statement.close();
//					}
//				} catch (SQLException e) {
//					log.warn(new SenderException(getLogPrefix(session) + "got exception closing SQL statement",e ));
//				}
//			}
		} catch (Exception e) {
			throw new SenderException(e);
//		} finally {
//			if (connection!=null) {
//				try {
//					connection.close();
//				} catch (SQLException e) {
//					log.warn(new SenderException(getLogPrefix(session) + "caught exception closing sender after sending message, ID=["+correlationID+"]", e));
//				}
//			}
		}
	}

	public void addParameter(Parameter p) {
		querySender.addParameter(p);
	}


	public void setQuery(String query) {
		querySender.setQuery(query);
	}
	public String getQuery() {
		return querySender.getQuery();
	}
	
	public void setDatasourceName(String datasourceName) {
		querySender.setDatasourceName(datasourceName);
	}
	public String getDatasourceName() {
		return querySender.getDatasourceName();
	}

	public String getPhysicalDestinationName() {
		return querySender.getPhysicalDestinationName();
	}

	public void setJmsRealm(String jmsRealmName) {
		querySender.setJmsRealm(jmsRealmName);
	}
	

}
