package org.frankframework.filesystem;

import static org.frankframework.filesystem.ISupportsCustomFileAttributes.FILE_ATTRIBUTE_PARAM_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

public abstract class FileSystemActorCustomFileAttributesTest<F, S extends IWritableFileSystem<F>> extends WritableFileSystemActorTest<F, S> {

	private Message input;

	@Override
	@AfterEach
	public void tearDown()  {
		CloseUtils.closeSilently(input, result);
		result = null;
		super.tearDown();
	}

	@Test
	public void testCreateWithCustomFileAttributes() throws Exception {
		// Arrange
		input = new Message("message-data");

		ParameterList parameters = new ParameterList();
		IParameter p1 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop1", "value1");
		IParameter p2 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop2", "välue2-non-ascii");
		IParameter p3 = new Parameter("prop3", "value3");
		parameters.add(p1);
		parameters.add(p2);
		parameters.add(p3);
		parameters.configure();
		ParameterValueList pvl = parameters.getValues(input, session);

		actor.setAction(FileSystemActor.FileSystemAction.CREATE);
		actor.configure(fileSystem, parameters, owner);
		actor.open();

		// Act
		result = actor.doAction(input, pvl, session);

		// Assert
		assertThat(result.asString(), containsString("prop1=\"value1\""));
		assertThat(result.asString(), containsString("prop2=\"välue2-non-ascii\""));
		assertThat(result.asString(), not(containsString("prop3=\"value3\"")));
	}

	@Test
	public void testWriteWithCustomFileAttributes() throws Exception {
		// Arrange
		input = new Message("message-data");

		ParameterList parameters = new ParameterList();
		IParameter p1 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop1", "value1");
		IParameter p2 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop2", "välue2-non-ascii");
		IParameter p3 = new Parameter("prop3", "value3");
		parameters.add(p1);
		parameters.add(p2);
		parameters.add(p3);
		parameters.configure();
		ParameterValueList pvl = parameters.getValues(input, session);

		actor.setAction(FileSystemActor.FileSystemAction.WRITE);
		actor.setFilename("message-data.txt");
		actor.configure(fileSystem, parameters, owner);
		actor.open();

		// Act
		result = actor.doAction(input, pvl, session);

		// Assert
		assertThat(result.asString(), containsString("prop1=\"value1\""));
		assertThat(result.asString(), containsString("prop2=\"välue2-non-ascii\""));
		assertThat(result.asString(), not(containsString("prop3=\"value3\"")));

		// Check also message contents
		actor.setAction(FileSystemActor.FileSystemAction.READ);

		Message result2 = actor.doAction(input, pvl, session);
		assertNotNull(result2);
		assertNotNull(result2.asString());
		assertEquals("message-data", result2.asString());
	}

	@Test
	public void testInfoWithCustomFileAttributes() throws Exception {
		// Arrange
		input = new Message("message-data");

		ParameterList parameters = new ParameterList();
		IParameter p1 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop1", "value1 with space");
		IParameter p2 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop2", "välüé2-€-nön-äscïï");
		IParameter p3 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop3", "välue3-non-ascii");
		IParameter p4 = new Parameter("prop4", "value4");
		parameters.add(p1);
		parameters.add(p2);
		parameters.add(p3);
		parameters.add(p4);
		parameters.configure();
		ParameterValueList pvl = parameters.getValues(input, session);

		actor.setAction(FileSystemActor.FileSystemAction.CREATE);
		actor.configure(fileSystem, parameters, owner);
		actor.open();

		actor.doAction(input, pvl, session);

		// Act
		actor.setAction(FileSystemActor.FileSystemAction.INFO);
		result = actor.doAction(input, pvl, session);

		// Assert
		assertThat(result.asString(), containsString("prop1=\"value1 with space\""));
		assertThat(result.asString(), containsString("prop2=\"välüé2-€-nön-äscïï\""));
		assertThat(result.asString(), containsString("prop3=\"välue3-non-ascii\""));
		assertThat(result.asString(), not(containsString("prop4=\"value4\"")));
	}
}
