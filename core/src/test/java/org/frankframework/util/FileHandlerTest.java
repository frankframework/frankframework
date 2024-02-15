package org.frankframework.util;

import java.util.stream.Stream;

import org.frankframework.filesystem.IFileHandler;
import org.junit.jupiter.params.provider.Arguments;

public class FileHandlerTest extends FileHandlerTestBase {
	public Class<? extends IFileHandler> implementation;

	protected static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of(FileHandlerWrapper.class)
//				Arguments.of(LocalFileSystemHandler.class) // TODO: figure out what this class of the past should be; this one doesn't exist anymore
		);
	}

}
