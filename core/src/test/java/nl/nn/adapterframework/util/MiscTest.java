package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Misc Tester.
 *
 * @author <Sina Sen>
 */
public class MiscTest {

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();
    private static String sourceFolderPath;

    private static File file;

    private String pathSeperator = File.separator;

    @BeforeClass
    public static void setUp() throws IOException {
        sourceFolderPath = testFolder.getRoot().getPath();
        file = testFolder.newFile("lebron.txt");
        Writer w = new FileWriter(file.getName());
        w.write("inside the lebron file");
        w.close();
    }

    @AfterClass
    public static void cleanUp() {
        File f = new File("lebron.txt");
        f.delete();
    }


    @Test
    public void testStreamToString() throws IOException {
        String tekst = "dit is een string";
        ByteArrayInputStream bais = new ByteArrayInputStream(tekst.getBytes());

        CloseChecker closeChecker = new CloseChecker(bais);
        String actual = Misc.streamToString(closeChecker);

        assertEquals(tekst, actual);
        assertTrue("inputstream was not closed", closeChecker.inputStreamClosed);
    }

    private class CloseChecker extends FilterInputStream {

        boolean inputStreamClosed;

        public CloseChecker(InputStream arg0) {
            super(arg0);
        }

        @Override
        public void close() throws IOException {
            inputStreamClosed = true;

            super.close();
        }
    }

    /**
     * Method: createSimpleUUID()
     */
    @Test
    public void testCreateSimpleUUID() throws Exception {
        String uuid = Misc.createSimpleUUID();
        assertFalse(sourceFolderPath.isEmpty()); // for avoiding code quality warnings
        assertEquals(uuid.substring(8, 9), "-");
        assertFalse(uuid.isEmpty());
    }


    /**
     * Method: createRandomUUID(boolean removeDashes)
     */
    @Test
    public void testCreateRandomUUIDRemoveDashes() throws Exception {
        String uuid = Misc.createRandomUUID(true);
        assertNotEquals(uuid.substring(8, 9), "-"); // assert that dashes are removed
        assertEquals(uuid.length(), 32);
    }

    @Test
    public void testCreateRandomUUID() throws Exception {
        String uuid = Misc.createRandomUUID();
        assertFalse(uuid.isEmpty());
    }


    /**
     * Method: asHex(byte[] buf)
     */
    @Test
    public void testAsHex() throws Exception {
        String test = "test";
        String hex = Misc.asHex(test.getBytes());
        assertEquals(hex, "74657374");
    }

    /**
     * Method: createNumericUUID()
     */
    @Test
    public void testCreateNumericUUID() throws Exception {
        String uuid = Misc.createNumericUUID();
        assertEquals(uuid.length(), 31);    //Unique string is <ipaddress with length 4*3><currentTime with length 13><hashcode with length 6>
    }

    /**
     * Method: unsignedByteToInt(byte b)
     */
    @Test
    public void testUnsignedByteToInt() throws Exception {
        assertEquals(Misc.unsignedByteToInt(new Byte("-12")), 244);
        assertEquals(Misc.unsignedByteToInt(new Byte("12")), 12);

    }


    /**
     * Method: fileToWriter(String filename, Writer writer)
     */
    @Test
    public void testFileToWriter() throws Exception {
        Writer writer = new StringWriter();
        Misc.fileToWriter(file.getName(), writer);
        assertEquals(writer.toString(), "inside the lebron file");
    }

    /**
     * Method: fileToStream(String filename, OutputStream output)
     */
    @Test
    public void testFileToStream() throws Exception {
        OutputStream os = new ByteArrayOutputStream();
        Misc.fileToStream(file.getName(), os);
        assertEquals(os.toString(), "inside the lebron file");
    }

    /**
     * Method: streamToStream(InputStream input, OutputStream output)
     */
    @Test
    public void testStreamToStreamForInputOutput() throws Exception {
        String test = "test";
        ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
        OutputStream baos = new ByteArrayOutputStream();
        Misc.streamToStream(bais, baos);
        assertEquals(baos.toString(), "test");
    }

    /**
     * Method: streamToStream(InputStream input, OutputStream output, boolean closeInput)
     */
    @Test
    public void testStreamToStreamForInputOutputCloseInput() throws Exception {
        String test = "test";
        ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
        OutputStream baos = new ByteArrayOutputStream();
        Misc.streamToStream(bais, baos, true);
        assertEquals(baos.toString(), "test");
    }

    /**
     * Method: streamToFile(InputStream inputStream, File file)
     */
    @Test
    public void testStreamToFile() throws Exception {
        String test = "test";
        ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
        Misc.streamToFile(bais, file);

        // to read from the file
        InputStream is = new FileInputStream(file);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));

        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();

        while (line != null) {
            sb.append(line).append("\n");
            line = buf.readLine();
        }

        String fileAsString = sb.toString();
        assertEquals(fileAsString, "test\n");
    }

    /**
     * Method: streamToBytes(InputStream inputStream)
     */
    @Test
    public void testStreamToBytes() throws Exception {
        String test = "test";
        ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
        byte[] arr = Misc.streamToBytes(bais);
        assertEquals(new String(arr, StandardCharsets.UTF_8), "test");
    }

    /**
     * Method: readerToWriter(Reader reader, Writer writer)
     */
    @Test
    public void testReaderToWriterForReaderWriter() throws Exception {
        Reader reader = new StringReader("test");
        Writer writer = new StringWriter();
        Misc.readerToWriter(reader, writer);
        assertEquals(writer.toString(), "test");
    }


    /**
     * Method: fileToString(String fileName)
     */
    @Test
    public void testFileToStringFileName() throws Exception {
        //Misc.resourceToString()
        assertEquals(Misc.fileToString(file.getName()), "inside the lebron file");
    }
    @Test
    public void testFileToStringFileNameEndLine() throws Exception {
        //Misc.resourceToString()
        assertEquals(Misc.fileToString(file.getName(), " the end"), "inside the lebron file");
    }


    /**
     * Method: readerToString(Reader reader, String endOfLineString, boolean xmlEncode)
     */
    @Test
    public void testReaderToStringNoXMLEncode() throws Exception {
        Reader r = new StringReader("<root> \n" +
                "    <name>GeeksforGeeks</name> \n" +
                "    <address> \n" +
                "        <sector>142</sector> \n" +
                "        <location>Noida</location> \n" +
                "    </address> \n" +
                "</root> r");
        String s = Misc.readerToString(r, "23", false);
        assertEquals(s, "<root> 23    <name>GeeksforGeeks</name> 23    <address> 23        <sector>142</sector> 23        <location>Noida</location> 23    </address> 23</root> r");
    }

    /**
     * Method: readerToString(Reader reader, String endOfLineString, boolean xmlEncode)
     */
    @Test
    public void testReaderToStringXMLEncode() throws Exception {
        Reader r = new StringReader("<root> \n" +
                "    <name>GeeksforGeeks</name> \n" +
                "    <address> \n" +
                "        <sector>142</sector> \n" +
                "        <location>Noida</location> \n" +
                "    </address> \n" +
                "</root> r");
        String s = Misc.readerToString(r, "23", true);
        assertEquals(s, "&lt;root&gt; 23    &lt;name&gt;GeeksforGeeks&lt;/name&gt; 23    &lt;address&gt; 23        &lt;sector&gt;142&lt;/sector&gt; 23        &lt;location&gt;Noida&lt;/location&gt; 23    &lt;/address&gt; 23&lt;/root&gt; r");
    }



    /**
     * Method: replace(String source, String from, String to)
     */
    @Test
    public void testReplace() throws Exception {
        String a = "Kobe";
        String res = Misc.replace(a, "Ko", "Phoe");
        assertEquals(res, "Phoebe");
    }

    /**
     * Method: concatStrings(String part1, String separator, String part2)
     */
    @Test
    public void testConcatStrings() throws Exception {
        String a = "LeBron";
        String b = "James";
        String seperator = "//";
        String res = Misc.concatStrings(a, seperator, b);
        assertEquals(res, "LeBron//James");
    }

    /**
     * Method: hide(String string)
     */
    @Test
    public void testHideString() throws Exception {
        String a = "test";
        String res = Misc.hide(a);
        assertEquals(res, "****");
    }

    /**
     * Method: hide(String string, int mode)
     */
    @Test
    public void testHideForStringMode() throws Exception {
        String a = "test";
        String res = Misc.hide(a, 1);
        assertEquals(res, "t**t");
    }

    /**
     * Method: byteArrayToString(byte[] input, String endOfLineString, boolean xmlEncode)
     */
    @Test
    public void testByteArrayToString() throws Exception {
        String s = "test";
        byte[] arr = s.getBytes();
        String res = Misc.byteArrayToString(arr, "", true);
        assertEquals(res, s);
    }

    /**
     * Method: gzip(String input)
     */
    @Test
    public void testGzipGUnzipInput() throws Exception {
        String s = "test";
        byte[] arr = s.getBytes();
        byte[] zipped = Misc.gzip(arr);
        assertEquals(Misc.gunzipToString(zipped), "test");
    }

    @Test
    public void testGzipString() throws Exception {
        String s = "you deserved this";
        byte[] zipped = Misc.gzip(s);
        assertEquals(Misc.gunzipToString(zipped), "you deserved this");

    }

    /**
     * Method: compress(String input)
     */
    @Test
    public void testCompressDecompressInput() throws Exception {
        String s = "test";
        byte[] compressed = Misc.compress(s);
        String decompressed = Misc.decompressToString(compressed);
        assertEquals(decompressed, "test");
    }


    /**
     * Method: copyContext(String keys, Map<String,Object> from, Map<String,Object> to)
     */
    @Test
    public void testCopyContext() throws Exception {
        Map<String, Object> context1 = new HashMap<>();
        Map<String, Object> context2 = new HashMap<>();
        String keys = "a,b";
        context1.put("a", 15);
        context1.put("b", 16);
        Misc.copyContext(keys, context1, context2);
        assertTrue(context1.equals(context2));
    }

    /**
     * Method: toFileSize(String value, long defaultValue)
     */
    @Test
    public void testToFileSizeForValueDefaultValue() throws Exception {
        long res = Misc.toFileSize("14GB", 20);
        assertEquals(Long.toString(res), "15032385536");
    }

    /**
     * Method: toFileSize(long value)
     */
    @Test
    public void testToFileSizeValue() throws Exception {
        String kb = Misc.toFileSize(150000, false, true);
        String mb = Misc.toFileSize(15000000, true);
        String gb = Misc.toFileSize(Long.parseLong("3221225472"));
        assertEquals(gb, "3GB");
        assertEquals(mb, "14 MB");
        assertEquals(kb, "146KB");
    }


    @Test
    public void testListToStringWithStringList() {
        List list = new ArrayList<Integer>();
        list.add("bailar");
        list.add("besos");
        String res = Misc.listToString(list);
        assertEquals(res, "bailarbesos");
    }

    /**
     * Method: addItemsToList(Collection<String> collection, String list, String collectionDescription, boolean lowercase)
     */
    @Test
    public void testAddItemsToList() throws Exception {
        List<String> stringCollection = new ArrayList<>();
        String list = "a,b,C";
        String collectionDescription = "First 3 letters of the alphabet";
        Misc.addItemsToList(stringCollection, list, collectionDescription, true);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("a");  arrayList.add("b"); arrayList.add("c");
        assertTrue(stringCollection.size() == 3);
        assertEquals(stringCollection.get(stringCollection.size()-1), "c");
    }

    /**
     * Method: getFileSystemTotalSpace()
     */
    @Test
    public void testGetFileSystemTotalSpace() throws Exception {
assertFalse(Misc.getFileSystemTotalSpace().isEmpty());    }

    /**
     * Method: getFileSystemFreeSpace()
     */
    @Test
    public void testGetFileSystemFreeSpace() throws Exception {
assertFalse(Misc.getFileSystemFreeSpace().isEmpty());    }

    /**
     * Method: getAge(long value)
     */
    @Test
    public void testGetAge() throws Exception {
        assertFalse(Misc.getAge(1).isEmpty());
    }
    /**
     * Method: getDurationInMs(long value)
     */
    @Test
    public void testGetDurationInMs() throws Exception {
        assertFalse(Misc.getDurationInMs(14).isEmpty());
    }

    /**
     * Method: parseAge(String value, long defaultValue)
     */
    @Test
    public void testParseAge() throws Exception {
        long res = Misc.parseAge("2D", 100);
        assertEquals(res, 172800000);
    }

    /**
     * Method: cleanseMessage(String inputString, String hideRegex, String hideMethod)
     */
    @Test
    public void testCleanseMessage() throws Exception {
        String s = "Donald Duck 23  Hey hey  14  Wooo";
        String regex = "\\d";
        String res = Misc.cleanseMessage(s, regex, " does not matter");
        assertEquals(res, "Donald Duck **  Hey hey  **  Wooo");
        }

    /**
     * Method: hideFirstHalf(String inputString, String regex)
     */
    @Test
    public void testHideFirstHalf() throws Exception {
        String s = "Donald Duck     Hey hey     Wooo";
        String hideRegex = "[^\\s*].*[^\\s*]";
        String res = Misc.hideFirstHalf(s, hideRegex);
        assertEquals(res, "****************Hey hey     Wooo");

    }


    /**
     * Method: getBuildOutputDirectory()
     */
    @Test
    public void testGetBuildOutputDirectory() throws Exception {
        String outputDirectory = Misc.getBuildOutputDirectory();
assertEquals(outputDirectory.substring(outputDirectory.length()-29, outputDirectory.length()), pathSeperator+"iaf"+pathSeperator+"core"+pathSeperator+"target"+pathSeperator+"test-classes");    }

    /**
     * Method: getProjectBaseDir()
     */
    @Test
    public void testGetProjectBaseDir() throws Exception {
        String baseDir = Misc.getBuildOutputDirectory();
        assertEquals(baseDir.substring(baseDir.length()-29, baseDir.length()), pathSeperator+"iaf"+pathSeperator+"core"+pathSeperator+"target"+pathSeperator+"test-classes");
    }

    /**
     * Method: toSortName(String name)
     */
    @Test
    public void testToSortName() throws Exception {
assertEquals(Misc.toSortName("new_name"), "NEW*NAME");    }

    /**
     * Method: countRegex(String string, String regex)
     */
    @Test
    public void testCountRegex() throws Exception {
String s = "12ab34";
    String regex = "\\d";
    int regexCount = Misc.countRegex(s, regex);
    assertEquals(regexCount, 4);
    }

    /**
     * Method: resourceToString(URL resource)
     */
    @Test
    public void testResourceToStringResource() throws Exception {
        URL resource = new URL("http://example.com/");
        String s1 = Misc.resourceToString(resource);
        assertEquals(s1, "<!doctype html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Example Domain</title>\n" +
                "\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "    <style type=\"text/css\">\n" +
                "    body {\n" +
                "        background-color: #f0f0f2;\n" +
                "        margin: 0;\n" +
                "        padding: 0;\n" +
                "        font-family: -apple-system, system-ui, BlinkMacSystemFont, \"Segoe UI\", \"Open Sans\", \"Helvetica Neue\", Helvetica, Arial, sans-serif;\n" +
                "        \n" +
                "    }\n" +
                "    div {\n" +
                "        width: 600px;\n" +
                "        margin: 5em auto;\n" +
                "        padding: 2em;\n" +
                "        background-color: #fdfdff;\n" +
                "        border-radius: 0.5em;\n" +
                "        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);\n" +
                "    }\n" +
                "    a:link, a:visited {\n" +
                "        color: #38488f;\n" +
                "        text-decoration: none;\n" +
                "    }\n" +
                "    @media (max-width: 700px) {\n" +
                "        div {\n" +
                "            margin: 0 auto;\n" +
                "            width: auto;\n" +
                "        }\n" +
                "    }\n" +
                "    </style>    \n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "<div>\n" +
                "    <h1>Example Domain</h1>\n" +
                "    <p>This domain is for use in illustrative examples in documents. You may use this\n" +
                "    domain in literature without prior coordination or asking for permission.</p>\n" +
                "    <p><a href=\"https://www.iana.org/domains/example\">More information...</a></p>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>\n");
    }

    /**
     * Method: resourceToString(URL resource, String endOfLineString, boolean xmlEncode)
     */
    @Test
    public void testResourceToStringForResourceEndOfLineStringXmlEncode() throws Exception {
        URL resource = new URL("http://example.com/");
        String s1 = Misc.resourceToString(resource, "end of the page", true);
        assertEquals(s1, "&lt;!doctype html&gt;end of the page&lt;html&gt;end of the page&lt;head&gt;end of the page    &lt;title&gt;Example Domain&lt;/title&gt;end of the pageend of the page    &lt;meta charset=&quot;utf-8&quot; /&gt;end of the page    &lt;meta http-equiv=&quot;Content-type&quot; content=&quot;text/html; charset=utf-8&quot; /&gt;end of the page    &lt;meta name=&quot;viewport&quot; content=&quot;width=device-width, initial-scale=1&quot; /&gt;end of the page    &lt;style type=&quot;text/css&quot;&gt;end of the page    body {end of the page        background-color: #f0f0f2;end of the page        margin: 0;end of the page        padding: 0;end of the page        font-family: -apple-system, system-ui, BlinkMacSystemFont, &quot;Segoe UI&quot;, &quot;Open Sans&quot;, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;end of the page        end of the page    }end of the page    div {end of the page        width: 600px;end of the page        margin: 5em auto;end of the page        padding: 2em;end of the page        background-color: #fdfdff;end of the page        border-radius: 0.5em;end of the page        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);end of the page    }end of the page    a:link, a:visited {end of the page        color: #38488f;end of the page        text-decoration: none;end of the page    }end of the page    @media (max-width: 700px) {end of the page        div {end of the page            margin: 0 auto;end of the page            width: auto;end of the page        }end of the page    }end of the page    &lt;/style&gt;    end of the page&lt;/head&gt;end of the pageend of the page&lt;body&gt;end of the page&lt;div&gt;end of the page    &lt;h1&gt;Example Domain&lt;/h1&gt;end of the page    &lt;p&gt;This domain is for use in illustrative examples in documents. You may use thisend of the page    domain in literature without prior coordination or asking for permission.&lt;/p&gt;end of the page    &lt;p&gt;&lt;a href=&quot;https://www.iana.org/domains/example&quot;&gt;More information...&lt;/a&gt;&lt;/p&gt;end of the page&lt;/div&gt;end of the page&lt;/body&gt;end of the page&lt;/html&gt;end of the page");
    }

    @Test
    public void testResourceToStringForResourceEndOfLineString() throws Exception {
        URL resource = new URL("http://example.com/");
        String s1 = Misc.resourceToString(resource, "end of the page");
        assertEquals(s1, "<!doctype html>end of the page<html>end of the page<head>end of the page    <title>Example Domain</title>end of the pageend of the page    <meta charset=\"utf-8\" />end of the page    <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />end of the page    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />end of the page    <style type=\"text/css\">end of the page    body {end of the page        background-color: #f0f0f2;end of the page        margin: 0;end of the page        padding: 0;end of the page        font-family: -apple-system, system-ui, BlinkMacSystemFont, \"Segoe UI\", \"Open Sans\", \"Helvetica Neue\", Helvetica, Arial, sans-serif;end of the page        end of the page    }end of the page    div {end of the page        width: 600px;end of the page        margin: 5em auto;end of the page        padding: 2em;end of the page        background-color: #fdfdff;end of the page        border-radius: 0.5em;end of the page        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);end of the page    }end of the page    a:link, a:visited {end of the page        color: #38488f;end of the page        text-decoration: none;end of the page    }end of the page    @media (max-width: 700px) {end of the page        div {end of the page            margin: 0 auto;end of the page            width: auto;end of the page        }end of the page    }end of the page    </style>    end of the page</head>end of the pageend of the page<body>end of the page<div>end of the page    <h1>Example Domain</h1>end of the page    <p>This domain is for use in illustrative examples in documents. You may use thisend of the page    domain in literature without prior coordination or asking for permission.</p>end of the page    <p><a href=\"https://www.iana.org/domains/example\">More information...</a></p>end of the page</div>end of the page</body>end of the page</html>end of the page");
    }
}
