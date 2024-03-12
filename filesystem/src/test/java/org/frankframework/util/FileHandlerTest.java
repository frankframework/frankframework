package org.frankframework.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Be aware, this is a deprecated class test...
 */
public class FileHandlerTest extends FileHandlerTestBase {

	protected static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of(FileHandlerWrapper.class)
		);
	}

}
