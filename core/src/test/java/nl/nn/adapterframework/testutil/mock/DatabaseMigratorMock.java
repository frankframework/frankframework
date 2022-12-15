package nl.nn.adapterframework.testutil.mock;

import java.io.Writer;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;

public class DatabaseMigratorMock extends DatabaseMigratorBase {

	@Override
	public boolean validate() {
		return false;
	}

	@Override
	public void update() throws JdbcException {
		// TODO Auto-generated method stub
	}

	@Override
	public void update(Writer writer) throws JdbcException {
		// TODO Auto-generated method stub
	}

	@Override
	public void update(Writer writer, Resource resource) throws JdbcException {
		// TODO Auto-generated method stub
	}

	@Override
	public Resource getChangeLog() {
		return null;
	}

}
