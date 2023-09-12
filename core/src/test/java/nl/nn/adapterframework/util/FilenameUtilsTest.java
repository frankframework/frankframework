package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;


/**
 * FilenameUtils Tester.
 *
 * @author <Sina Sen>
 */
class FilenameUtilsTest {

	@Test
	void testNormalizeFilename() {
		assertEquals(File.separator + "baz", FilenameUtils.normalize("/foo/../bar/../baz"));
	}

	@Test
	void testNormalizeForFilenameUnixSeparator() {
		assertEquals("C:/bar", FilenameUtils.normalize("C:\\foo\\..\\bar", true));
	}

	@Test
	void testNormalizeNoEndSeparatorFilename() {
		assertEquals(File.separator + "foo", FilenameUtils.normalizeNoEndSeparator("/foo//"));
	}

	@Test
	void testNormalizeNullInout() {
		assertNull(FilenameUtils.normalize(null));
	}

	@Test
	void testConcat() {
		assertEquals("src" + File.separator + "test" + File.separator + "java" + File.separator + "nl" + File.separator + "nn"
				+ File.separator + "adapterframework" + File.separator + "util" + File.separator + "test" + File.separator + "test.java", FilenameUtils.concat("src\\test\\java\\nl\\nn\\adapterframework\\util", "test\\test.java"));
	}

	@Test
	void testSeparatorsToUnix() {
		assertEquals("src/test/java/nl/nn/adapterframework/util", FilenameUtils.separatorsToUnix("src\\test\\java\\nl\\nn\\adapterframework\\util"));
	}

	@Test
	void testSeparatorsToWindows() {
		assertEquals("src\\test\\java\\nl\\nn\\adapterframework\\util", FilenameUtils.separatorsToWindows("src/test/java/nl/nn/adapterframework/util"));
	}

	@Test
	void testSeparatorsToSystem() {
		if (FilenameUtils.isSystemWindows()) {
			testSeparatorsToWindows();
		} else {
			testSeparatorsToUnix();
		}
	}

	@Test
	void testGetPrefixLength() {
		assertEquals(0, FilenameUtils.getPrefixLength("src/test/java/nl/nn/adapterframework/util/t.txt"));
		assertEquals(7, FilenameUtils.getPrefixLength("~userz/a/b/c.txt"));

	}

	@Test
	void testIndexOfLastSeparator() {
		assertEquals(41, FilenameUtils.indexOfLastSeparator("src\\test\\java\\nl\\nn\\adapterframework\\util\\t.txt"));
	}

	@Test
	void testIndexOfExtension() {
		assertEquals(15, FilenameUtils.indexOfExtension("src/blabla/text.txt"));
	}

	@Test
	void testGetPrefix() {
		assertEquals("~userz/", FilenameUtils.getPrefix("~userz/a/b/c.txt"));
	}

	@Test
	void testGetPath() {
		assertEquals("a/b/", FilenameUtils.getPath("~userz/a/b/c.txt"));
	}


	@Test
	void testGetPathNoEndSeparator() {
		assertEquals("a\\b", FilenameUtils.getPathNoEndSeparator("C:\\a\\b\\c.txt"));
	}

	@Test
	void testGetFullPath() {
		assertEquals("C:\\a\\b\\", FilenameUtils.getFullPath("C:\\a\\b\\c.txt"));

	}

	@Test
	void testGetFullPathNoEndSeparator() {
		assertEquals("C:\\a\\b", FilenameUtils.getFullPathNoEndSeparator("C:\\a\\b\\c.txt"));

	}

	@Test
	void testGetName() {
		assertEquals("c.txt", FilenameUtils.getName("C:\\a\\b\\c.txt"));
	}

	@Test
	void testGetBaseName() {
		assertEquals("c", FilenameUtils.getBaseName("C:\\a\\b\\c.txt"));
	}

	@Test
	void testGetExtension() {
		assertEquals("txt", FilenameUtils.getExtension("C:\\a\\b\\c.txt"));
	}

	@Test
	void testRemoveExtension() {
		assertEquals("C:\\a\\b\\c", FilenameUtils.removeExtension("C:\\a\\b\\c.txt"));
	}

	@Test
	void testEqualsForFilename1Filename2() {
		assertTrue(FilenameUtils.equals("C:\\a\\b\\c.txt", "C:\\a\\b\\c.txt"));
	}

	@Test
	void testEqualsOnSystem() {
		if (FilenameUtils.isSystemWindows()) {
			assertTrue(FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"));
		} else {
			assertNotEquals(true, FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"));
		}
	}

	@Test
	void testEqualsNormalized() {
		assertTrue(FilenameUtils.equalsNormalized("/foo//", "/foo/./"));
	}

	@Test
	void testEqualsNormalizedOnSystem() {
		if (FilenameUtils.isSystemWindows()) {
			assertTrue(FilenameUtils.equalsNormalizedOnSystem("/fOO//", "/foo/./"));
		} else {
			testEqualsNormalized();
		}
	}

	@Test
	void testIsExtensionForFilenameExtension() {
		assertTrue(FilenameUtils.isExtension("C:\\a\\b\\c.txt", "txt"));
	}

	@Test
	void testIsExtensionForFilenameExtensions() {
		String[] extensions = {"txt", "js"};
		assertTrue(FilenameUtils.isExtension("C:\\a\\b\\c.txt", extensions));
	}

	@Test
	void testWildcardMatchForFilenameWildcardMatcher() {
		assertTrue(FilenameUtils.wildcardMatch("C:\\a\\b\\c.txt", "*.txt"));
	}

	@Test
	void testWildcardMatchOnSystem() {
		if (FilenameUtils.isSystemWindows()) {
			assertTrue(FilenameUtils.wildcardMatch("C:\\a\\b\\C.txt", "*.txt"));
		} else {
			testWildcardMatchForFilenameWildcardMatcher();
		}
	}

}
