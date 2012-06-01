/*
 * $Log: BatchTransformerPipeBase.java,v $
 * Revision 1.7  2012-06-01 10:52:52  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2012/02/17 18:04:02  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Use proxiedDataSources for JdbcIteratingPipeBase too
 * Call close on original/proxied connection instead of connection from statement that might be the unproxied connection
 *
 * Revision 1.5  2011/11/30 13:51:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/04/13 08:27:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Indicate updatability of resultset explicitly using method-parameter
 *
 * Revision 1.2  2010/05/03 17:04:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked stream handling, to allow for binary records.
 *
 * Revision 1.1  2007/08/03 08:44:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed TransformingPipes to TransformerPipes
 *
 * Revision 1.1  2007/07/26 16:16:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import nl.nn.adapterframework.batch.StreamTransformerPipe;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * abstract base class for JDBC batch transforming pipes.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class BatchTransformerPipeBase extends StreamTransformerPipe {
	
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

	public class ResultSetReader extends BufferedReader {
		Connection conn;
		ResultSet rs;

		ResultSetReader(Connection conn, ResultSet rs, Reader reader) {
			super(reader);
			this.conn=conn;
			this.rs=rs;
		}

		public void close() throws IOException {
			try {
				super.close();
			} finally {
				JdbcUtil.fullClose(conn, rs);
			}
		}
		
	}

	protected abstract Reader getReader(ResultSet rs, String charset, String streamId, IPipeLineSession session) throws SenderException;

	protected BufferedReader getReader(String streamId, Object input, IPipeLineSession session) throws PipeRunException {
		Connection connection = null;
		try {
			connection = querySender.getConnection();
			PreparedStatement statement=null;
			String msg = (String)input;
			statement = querySender.getStatement(connection, streamId, msg, false);
			ParameterResolutionContext prc = new ParameterResolutionContext(msg,session);
			if (querySender.paramList != null) {
				querySender.applyParameters(statement, prc.getValues(querySender.paramList));
			}
			ResultSet rs = statement.executeQuery();
			if (rs==null || !rs.next()) {
				throw new SenderException("query has empty resultset");
			}
			return new ResultSetReader(connection, rs,getReader(rs, getCharset(), streamId, session));
		} catch (Exception e) {
			throw new PipeRunException(this,"cannot open reader",e);
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
