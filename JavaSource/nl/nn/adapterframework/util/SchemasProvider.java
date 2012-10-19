package nl.nn.adapterframework.util;

import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/*
 * When getSchemasId() returns a value the schemas loaded at initialisation time
 * will be used to validate otherwise the run time schemas are loaded and used.
 */
public interface SchemasProvider {

	/*
	 * Id of schemas to load at initialisation time.
	 */
	public String getSchemasId() throws ConfigurationException;

	/*
	 * Schemas to load at initialisation time.
	 */
	public List<Schema> getSchemas() throws ConfigurationException;

	/*
	 * Id of schemas to load at run time.
	 */
	public String getSchemasId(IPipeLineSession session) throws PipeRunException;

	/*
	 * Schemas to load at run time.
	 */
	public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException;
}
