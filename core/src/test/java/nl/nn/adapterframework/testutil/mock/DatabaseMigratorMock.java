package nl.nn.adapterframework.testutil.mock;

import java.io.Writer;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;

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
		// Nothing to update
	}

	@Override
	public Resource getChangeLog() {
		return Resource.getResource(this, "/Migrator/DatabaseChangelog.xml");
	}

}
