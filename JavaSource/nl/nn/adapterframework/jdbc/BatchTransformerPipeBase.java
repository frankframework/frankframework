/*
 * $Log: BatchTransformerPipeBase.java,v $
 * Revision 1.1  2007-08-03 08:44:05  europe\L190409
 * renamed TransformingPipes to TransformerPipes
 *
 * Revision 1.1  2007/07/26 16:16:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import nl.nn.adapterframework.batch.StreamTransformerPipe;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
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

	public class ResultSetReader extends Reader {
		ResultSet rs;		
		Reader reader;

		ResultSetReader(ResultSet rs, Reader reader) {
			super();
			this.rs=rs;
			this.reader=reader;
		}

		public int read(char[] cbuf, int off, int len) throws IOException {
			return reader.read(cbuf,off,len);
		}

		public void close() throws IOException {
			try {
				reader.close();
			} finally {
				JdbcUtil.fullClose(rs);
			}
		}
		
	}

	protected abstract Reader getReader(ResultSet rs) throws SenderException;

	protected Reader getReader(String streamId, Object input, PipeLineSession session) throws PipeRunException {
		Connection connection = null;
		try {
			connection = querySender.getConnection();
			PreparedStatement statement=null;
			String msg = (String)input;
			statement = querySender.getStatement(connection, streamId, msg);
			ParameterResolutionContext prc = new ParameterResolutionContext(msg,session);
			if (prc != null && querySender.paramList != null) {
				querySender.applyParameters(statement, prc.getValues(querySender.paramList));
			}
			ResultSet rs = statement.executeQuery();
			if (rs==null || !rs.next()) {
				throw new SenderException("query has empty resultset");
			}
			return new ResultSetReader(rs,getReader(rs));
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
