package nl.nn.adapterframework.testutil.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.util.StreamUtil;

public class DatabaseMigratorMock extends DatabaseMigratorBase {

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public void update() throws JdbcException {
		// Nothing to update
	}

	@Override
	public void update(Writer writer) throws JdbcException {
		// Nothing to update
	}

	@Override
	public void update(Writer writer, Resource resource) throws JdbcException {
		try (InputStreamReader reader = new InputStreamReader(resource.openStream())) {
			StreamUtil.readerToWriter(reader, writer);
		} catch (IOException e) {
			throw new JdbcException("unable to write resource contents to write", e);
		}
	}

	@Override
	public Resource getChangeLog() {
		return Resource.getResource(this, "/Migrator/DatabaseChangelog.xml");
	}

}
