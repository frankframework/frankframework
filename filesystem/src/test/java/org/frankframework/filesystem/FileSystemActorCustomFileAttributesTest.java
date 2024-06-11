package org.frankframework.filesystem;

import static org.frankframework.filesystem.ISupportsCustomFileAttributes.FILE_ATTRIBUTE_PARAM_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

abstract public class FileSystemActorCustomFileAttributesTest<F, FS extends IWritableFileSystem<F>> extends FileSystemActorTest<F, FS> {


	@Test
	public void testCreateWithCustomFileAttributes() throws Exception {
		// Arrange
		Message input = new Message("message-data");

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
		Message result = actor.doAction(input, pvl, session);

		// Assert
		assertThat(result.asString(), containsString("prop1=\"value1\""));
		assertThat(result.asString(), containsString("prop2=\"välue2-non-ascii\""));
		assertThat(result.asString(), not(containsString("prop3=\"value3\"")));
	}

	@Test
	public void testInfoWithCustomFileAttributes() throws Exception {
		// Arrange
		Message input = new Message("message-data");

		ParameterList parameters = new ParameterList();
		IParameter p1 = new Parameter(FILE_ATTRIBUTE_PARAM_PREFIX + "prop1", "value1");
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
		actor.setAction(FileSystemActor.FileSystemAction.INFO);

		// Act
		Message result = actor.doAction(input, pvl, session);

		// Assert
		assertThat(result.asString(), containsString("prop1=\"value1\""));
		assertThat(result.asString(), containsString("prop2=\"välüé2-€-nön-äscïï\""));
		assertThat(result.asString(), containsString("prop3=\"välue3-non-ascii\""));
		assertThat(result.asString(), not(containsString("prop4=\"value4\"")));
	}
}
