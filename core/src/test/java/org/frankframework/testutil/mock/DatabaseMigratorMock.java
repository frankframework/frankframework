package org.frankframework.testutil.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import org.frankframework.core.Resource;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.migration.AbstractDatabaseMigrator;
import org.frankframework.util.StreamUtil;

public class DatabaseMigratorMock extends AbstractDatabaseMigrator {

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
