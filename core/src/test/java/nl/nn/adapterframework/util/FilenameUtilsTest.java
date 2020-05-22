package nl.nn.adapterframework.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * FilenameUtils Tester.
 *
 * @author <Sina Sen>
 */
public class FilenameUtilsTest {



    /**
     * Method: isSystemWindows()
     */
    @Test
    public void testIsSystemWindows() throws Exception {
        assertEquals(FilenameUtils.isSystemWindows(), true);
    }

    /**
     * Method: normalize(String filename)
     */
    @Test
    public void testNormalizeFilename() throws Exception {
        assertEquals(FilenameUtils.normalize("/foo/../bar/../baz"), "\\baz");
    }

    /**
     * Method: normalize(String filename, boolean unixSeparator)
     */
    @Test
    public void testNormalizeForFilenameUnixSeparator() throws Exception {
        assertEquals(FilenameUtils.normalize("C:\\foo\\..\\bar", true), "C:/bar");
    }

    /**
     * Method: normalizeNoEndSeparator(String filename)
     */
    @Test
    public void testNormalizeNoEndSeparatorFilename() throws Exception {
        assertEquals(FilenameUtils.normalizeNoEndSeparator("/foo//"), "\\foo");
    }

    @Test
    public void testNormalizeNullInout() {
        assertEquals(FilenameUtils.normalize(null), null);
    }

    /**
     * Method: concat(String basePath, String fullFilenameToAdd)
     */
    @Test
    public void testConcat() throws Exception {
        assertEquals(FilenameUtils.concat("src\\test\\java\\nl\\nn\\adapterframework\\util", "test\\test.java"), "src\\test\\java\\nl\\nn\\adapterframework\\util\\test\\test.java");
    }

    /**
     * Method: separatorsToUnix(String path)
     */
    @Test
    public void testSeparatorsToUnix() throws Exception {
        assertEquals(FilenameUtils.separatorsToUnix("src\\test\\java\\nl\\nn\\adapterframework\\util"), "src/test/java/nl/nn/adapterframework/util");
    }

    /**
     * Method: separatorsToWindows(String path)
     */
    @Test
    public void testSeparatorsToWindows() throws Exception {
        assertEquals(FilenameUtils.separatorsToWindows("src/test/java/nl/nn/adapterframework/util"), "src\\test\\java\\nl\\nn\\adapterframework\\util");
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
        assertEquals(FilenameUtils.getPrefixLength("src/test/java/nl/nn/adapterframework/util/t.txt"), 0);
        assertEquals(FilenameUtils.getPrefixLength("~userz/a/b/c.txt"), 7);

    }

    /**
     * Method: indexOfLastSeparator(String filename)
     */
    @Test
    public void testIndexOfLastSeparator() throws Exception {
        assertEquals(FilenameUtils.indexOfLastSeparator("src\\test\\java\\nl\\nn\\adapterframework\\util\\t.txt"), 41);
    }

    /**
     * Method: indexOfExtension(String filename)
     */
    @Test
    public void testIndexOfExtension() throws Exception {
        assertEquals(FilenameUtils.indexOfExtension("src/blabla/text.txt"), 15);
    }

    /**
     * Method: getPrefix(String filename)
     */
    @Test
    public void testGetPrefix() throws Exception {
        assertEquals(FilenameUtils.getPrefix("~userz/a/b/c.txt"), "~userz/");
    }

    /**
     * Method: getPath(String filename)
     */
    @Test
    public void testGetPath() throws Exception {
        assertEquals(FilenameUtils.getPath("~userz/a/b/c.txt"), "a/b/");
    }


    /**
     * Method: getPathNoEndSeparator(String filename)
     */
    @Test
    public void testGetPathNoEndSeparator() throws Exception {
        assertEquals(FilenameUtils.getPathNoEndSeparator("C:\\a\\b\\c.txt"), "a\\b");
    }

    /**
     * Method: getFullPath(String filename)
     */
    @Test
    public void testGetFullPath() throws Exception {
        assertEquals(FilenameUtils.getFullPath("C:\\a\\b\\c.txt"), "C:\\a\\b\\");

    }

    /**
     * Method: getFullPathNoEndSeparator(String filename)
     */
    @Test
    public void testGetFullPathNoEndSeparator() throws Exception {
        assertEquals(FilenameUtils.getFullPathNoEndSeparator("C:\\a\\b\\c.txt"), "C:\\a\\b");

    }

    /**
     * Method: getName(String filename)
     */
    @Test
    public void testGetName() throws Exception {
        assertEquals(FilenameUtils.getName("C:\\a\\b\\c.txt"), "c.txt");
    }

    /**
     * Method: getBaseName(String filename)
     */
    @Test
    public void testGetBaseName() throws Exception {
        assertEquals(FilenameUtils.getBaseName("C:\\a\\b\\c.txt"), "c");
    }

    /**
     * Method: getExtension(String filename)
     */
    @Test
    public void testGetExtension() throws Exception {
        assertEquals(FilenameUtils.getExtension("C:\\a\\b\\c.txt"), "txt");
    }

    /**
     * Method: removeExtension(String filename)
     */
    @Test
    public void testRemoveExtension() throws Exception {
        assertEquals(FilenameUtils.removeExtension("C:\\a\\b\\c.txt"), "C:\\a\\b\\c");

    }

    /**
     * Method: equals(String filename1, String filename2)
     */
    @Test
    public void testEqualsForFilename1Filename2() throws Exception {
assertEquals(FilenameUtils.equals("C:\\a\\b\\c.txt", "C:\\a\\b\\c.txt"), true);
    }

    /**
     * Method: equalsOnSystem(String filename1, String filename2)
     */
    @Test
    public void testEqualsOnSystem() throws Exception {
if(FilenameUtils.isSystemWindows()){
    assertEquals(FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"), true);
}else{
    assertNotEquals(FilenameUtils.equalsOnSystem("C:\\a\\b\\c.txt", "C:\\A\\B\\c.txt"), true);
}
    }

    /**
     * Method: equalsNormalized(String filename1, String filename2)
     */
    @Test
    public void testEqualsNormalized() throws Exception {
assertEquals(FilenameUtils.equalsNormalized("/foo//", "/foo/./"), true);    }

    /**
     * Method: equalsNormalizedOnSystem(String filename1, String filename2)
     */
    @Test
    public void testEqualsNormalizedOnSystem() throws Exception {
        if(FilenameUtils.isSystemWindows()) {
            assertEquals(FilenameUtils.equalsNormalizedOnSystem("/fOO//", "/foo/./"), true);
        }else{
            testEqualsNormalized();
        }
    }

    /**
     * Method: isExtension(String filename, String extension)
     */
    @Test
    public void testIsExtensionForFilenameExtension() throws Exception {
assertEquals(FilenameUtils.isExtension("C:\\a\\b\\c.txt", "txt"), true);
    }

    /**
     * Method: isExtension(String filename, String[] extensions)
     */
    @Test
    public void testIsExtensionForFilenameExtensions() throws Exception {
String[] extensions = {"txt", "js"};
        assertEquals(FilenameUtils.isExtension("C:\\a\\b\\c.txt", extensions), true);

    }

    /**
     * Method: wildcardMatch(String filename, String wildcardMatcher)
     */
    @Test
    public void testWildcardMatchForFilenameWildcardMatcher() throws Exception {
assertEquals(FilenameUtils.wildcardMatch("C:\\a\\b\\c.txt", "*.txt"), true);    }

    /**
     * Method: wildcardMatchOnSystem(String filename, String wildcardMatcher)
     */
    @Test
    public void testWildcardMatchOnSystem() throws Exception {
if(FilenameUtils.isSystemWindows()){
    assertEquals(FilenameUtils.wildcardMatch("C:\\a\\b\\C.txt", "*.txt"), true);    }
else{
    testWildcardMatchForFilenameWildcardMatcher();
    }
    }





} 
