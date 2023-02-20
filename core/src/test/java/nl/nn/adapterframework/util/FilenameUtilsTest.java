package nl.nn.adapterframework.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * FilenameUtils Tester.
 *
 * @author <Sina Sen>
 */
public class FilenameUtilsTest {


    /**
     * Method: normalize(String filename)
     */
    @Test
    public void testNormalizeFilename() throws Exception {
        assertEquals(File.separator + "baz", FilenameUtils.normalize("/foo/../bar/../baz"));
    }

    /**
     * Method: normalize(String filename, boolean unixSeparator)
     */
    @Test
    public void testNormalizeForFilenameUnixSeparator() throws Exception {
        assertEquals("C:/bar", FilenameUtils.normalize("C:\\foo\\..\\bar", true));
    }

    /**
     * Method: normalizeNoEndSeparator(String filename)
     */
    @Test
    public void testNormalizeNoEndSeparatorFilename() throws Exception {
        assertEquals(File.separator + "foo", FilenameUtils.normalizeNoEndSeparator("/foo//"));
    }

    @Test
    public void testNormalizeNullInout() {
        assertEquals(null, FilenameUtils.normalize(null));
    }

    /**
     * Method: concat(String basePath, String fullFilenameToAdd)
     */
    @Test
    public void testConcat() throws Exception {
        assertEquals("src" + File.separator + "test" + File.separator + "java" + File.separator + "nl" + File.separator + "nn"
                + File.separator + "adapterframework" + File.separator + "util" + File.separator + "test" + File.separator + "test.java", FilenameUtils.concat("src\\test\\java\\nl\\nn\\adapterframework\\util", "test\\test.java"));
    }

    /**
     * Method: separatorsToUnix(String path)
     */
    @Test
    public void testSeparatorsToUnix() throws Exception {
        assertEquals("src/test/java/nl/nn/adapterframework/util", FilenameUtils.separatorsToUnix("src\\test\\java\\nl\\nn\\adapterframework\\util"));
    }

    /**
     * Method: separatorsToWindows(String path)
     */
    @Test
    public void testSeparatorsToWindows() throws Exception {
        assertEquals("src\\test\\java\\nl\\nn\\adapterframework\\util", FilenameUtils.separatorsToWindows("src/test/java/nl/nn/adapterframework/util"));
    }

    /**
     * Method: separatorsToSystem(String path)
     */
    @Test
    public void testSeparatorsToSystem() throws Exception {
        if (FilenameUtils.isSystemWindows()) {
            testSeparatorsToWindows();
        } else {
            testSeparatorsToUnix();
        }
    }

    /**
     * Method: getPrefixLength(String filename)
     */
    @Test
    public void testGetPrefixLength() throws Exception {
        assertEquals(0, FilenameUtils.getPrefixLength("src/test/java/nl/nn/adapterframework/util/t.txt"));
        assertEquals(7, FilenameUtils.getPrefixLength("~userz/a/b/c.txt"));

    }

    /**
     * Method: indexOfLastSeparator(String filename)
     */
    @Test
    public void testIndexOfLastSeparator() throws Exception {
        assertEquals(41, FilenameUtils.indexOfLastSeparator("src\\test\\java\\nl\\nn\\adapterframework\\util\\t.txt"));
    }

    /**
     * Method: indexOfExtension(String filename)
     */
    @Test
    public void testIndexOfExtension() throws Exception {
        assertEquals(15, FilenameUtils.indexOfExtension("src/blabla/text.txt"));
    }

    /**
     * Method: getPrefix(String filename)
     */
    @Test
    public void testGetPrefix() throws Exception {
        assertEquals("~userz/", FilenameUtils.getPrefix("~userz/a/b/c.txt"));
    }

    /**
     * Method: getPath(String filename)
     */
    @Test
    public void testGetPath() throws Exception {
        assertEquals("a/b/", FilenameUtils.getPath("~userz/a/b/c.txt"));
    }


    /**
     * Method: getPathNoEndSeparator(String filename)
     */
    @Test
    public void testGetPathNoEndSeparator() throws Exception {
        assertEquals("a\\b", FilenameUtils.getPathNoEndSeparator("C:\\a\\b\\c.txt"));
    }

    /**
     * Method: getFullPath(String filename)
     */
    @Test
    public void testGetFullPath() throws Exception {
        assertEquals("C:\\a\\b\\", FilenameUtils.getFullPath("C:\\a\\b\\c.txt"));

    }

    /**
     * Method: getFullPathNoEndSeparator(String filename)
     */
    @Test
    public void testGetFullPathNoEndSeparator() throws Exception {
        assertEquals("C:\\a\\b", FilenameUtils.getFullPathNoEndSeparator("C:\\a\\b\\c.txt"));

    }

    /**
     * Method: getName(String filename)
     */
    @Test
    public void testGetName() throws Exception {
        assertEquals("c.txt", FilenameUtils.getName("C:\\a\\b\\c.txt"));
    }

    /**
     * Method: getBaseName(String filename)
     */
    @Test
    public void testGetBaseName() throws Exception {
        assertEquals("c", FilenameUtils.getBaseName("C:\\a\\b\\c.txt"));
    }

    /**
     * Method: getExtension(String filename)
     */
    @Test
    public void testGetExtension() throws Exception {
        assertEquals("txt", FilenameUtils.getExtension("C:\\a\\b\\c.txt"));
    }

    /**
     * Method: removeExtension(String filename)
     */
    @Test
    public void testRemoveExtension() throws Exception {
        assertEquals("C:\\a\\b\\c", FilenameUtils.removeExtension("C:\\a\\b\\c.txt"));

    }

    /**
     * Method: equals(String filename1, String filename2)
     */
    @Test
    public void testEqualsForFilename1Filename2() throws Exception {
        assertEquals(true, FilenameUtils.equals("C:\\a\\b\\c.txt", "C:\\a\\b\\c.txt"));
    }

    /**
     * Method: equalsOnSystem(String filename1, String filename2)
     */
    @Test
    public void testEqualsOnSystem() throws Exception {
        if (FilenameUtils.isSystemWindows()) {
            assertEquals(true, FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"));
        } else {
            assertNotEquals(true, FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"));
        }
    }

    /**
     * Method: equalsNormalized(String filename1, String filename2)
     */
    @Test
    public void testEqualsNormalized() throws Exception {
        assertEquals( true, FilenameUtils.equalsNormalized("/foo//", "/foo/./"));
    }

    /**
     * Method: equalsNormalizedOnSystem(String filename1, String filename2)
     */
    @Test
    public void testEqualsNormalizedOnSystem() throws Exception {
        if (FilenameUtils.isSystemWindows()) {
            assertEquals( true, FilenameUtils.equalsNormalizedOnSystem("/fOO//", "/foo/./"));
        } else {
            testEqualsNormalized();
        }
    }

    /**
     * Method: isExtension(String filename, String extension)
     */
    @Test
    public void testIsExtensionForFilenameExtension() throws Exception {
        assertEquals( true, FilenameUtils.isExtension("C:\\a\\b\\c.txt", "txt"));
    }

    /**
     * Method: isExtension(String filename, String[] extensions)
     */
    @Test
    public void testIsExtensionForFilenameExtensions() throws Exception {
        String[] extensions = {"txt", "js"};
        assertEquals( true, FilenameUtils.isExtension("C:\\a\\b\\c.txt", extensions));

    }

    /**
     * Method: wildcardMatch(String filename, String wildcardMatcher)
     */
    @Test
    public void testWildcardMatchForFilenameWildcardMatcher() throws Exception {
        assertEquals( true, FilenameUtils.wildcardMatch("C:\\a\\b\\c.txt", "*.txt"));
    }

    /**
     * Method: wildcardMatchOnSystem(String filename, String wildcardMatcher)
     */
    @Test
    public void testWildcardMatchOnSystem() throws Exception {
        if (FilenameUtils.isSystemWindows()) {
            assertEquals( true, FilenameUtils.wildcardMatch("C:\\a\\b\\C.txt", "*.txt"));
        } else {
            testWildcardMatchForFilenameWildcardMatcher();
        }
    }


}
