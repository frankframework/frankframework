package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

abstract public class FileSystemActorCustomPropertiesTest<F, FS extends IWritableFileSystem<F>> extends FileSystemActorTest<F, FS> {


	@Test
	public void testCreateWithCustomProperties() throws Exception {
		// Arrange
		IHasCustomProperties cp = (IHasCustomProperties) fileSystem;
		cp.setCustomPropertyNames(Set.of("prop1", "prop2"));

		Message input = new Message("message-data");

		ParameterList parameters = new ParameterList();
		IParameter p1 = new Parameter("prop1", "value1");
		IParameter p2 = new Parameter("prop2", "value2");
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
		assertThat(result.asString(), containsString("prop2=\"value2\""));
		assertThat(result.asString(), not(containsString("prop3=\"value3\"")));
	}
}
