package nl.nn.adapterframework.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.filesystem.IFileHandler;

@RunWith(value = Parameterized.class)
public class FileHandlerTest extends FileHandlerTestBase {

	@Parameterized.Parameter(0)
	public Class<? extends IFileHandler> implementation;

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { FileHandlerWrapper.class }
				// ,{LocalFileSystemHandler.class}
		};
		return Arrays.asList(data);
	}

	@Override
	protected IFileHandler createFileHandler() throws IllegalAccessException, InstantiationException {
		return implementation.newInstance();
	}

}
