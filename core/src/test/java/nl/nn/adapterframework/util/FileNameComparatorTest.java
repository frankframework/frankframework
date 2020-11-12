package nl.nn.adapterframework.util;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FileNameComparator Tester.
 *
 * @author <Sina Sen>
 */
public class FileNameComparatorTest {

	@Test
	public void testComparator() throws Exception {
		List<File> list = new ArrayList<>();
		list.add(new File("test.txt"));
		list.add(new File("teSt.txt"));
		list.add(new File("0123.py"));
		list.add(new File("test.xml"));
		list.add(new File("document.txt"));
		Collections.sort(list, new FileNameComparator());
		assertEquals("0123.py", list.get(0).getName());
		assertEquals("document.txt", list.get(1).getName());
		assertEquals("teSt.txt", list.get(2).getName());
		assertEquals("test.txt", list.get(3).getName());
		assertEquals("test.xml", list.get(4).getName());

	}

	/**
	 * Method: skipWhitespace(String s, int start)
	 */
	@Test
	public void testSkipWhitespace() throws Exception {
		int i = FileNameComparator.skipWhitespace("   file.txt", 0);
		assertEquals(3, i);
	}

	/**
	 * Method: compareStringsNaturalOrder(String s0, String s1, boolean caseSensitive)
	 */
	@Test
	public void testCompareStringsNaturalOrder() throws Exception {
		String s1 = "file1.txt";
		String s2 = "File2.txt";
		String s4 = "file2.txt";
		assertTrue(FileNameComparator.compareStringsNaturalOrder(s1, s2, true) > 0);
		assertTrue(FileNameComparator.compareStringsNaturalOrder(s2, s4, false) == 0);
	}

	/**
	 * Method: compareFilenames(File f0, File f1)
	 */
	@Test
	public void testCompareFilenames() throws Exception {
		File f1 = new File("first.txt");
		File f2 = new File("second.txt");
		File f3 = new File("first.txt");
		File f4 = new File("FiRst.txt");
		File f5 = new File("filename5.txt");
		File f6 = new File("filename24.txt");
		assertTrue(FileNameComparator.compareFilenames(f1, f2) < 0);
		assertTrue(FileNameComparator.compareFilenames(f1, f3) == 0);
		assertTrue(FileNameComparator.compareFilenames(f1, f4) > 0);
		assertTrue(FileNameComparator.compareFilenames(f2, f1) > 0);
		assertTrue(FileNameComparator.compareFilenames(f5, f6) < 0);
	}

	@Test
	public void testEndingWithNumber() throws Exception {
		List<File> list = new ArrayList<>();
		list.add(new File("ibis_xml.log.1"));
		list.add(new File("ibis_xml.log.2"));
		list.add(new File("ibis_xml.log.-2"));
		Collections.sort(list, new FileNameComparator());
		assertEquals("ibis_xml.log.1", list.get(0).getName());
		assertEquals("ibis_xml.log.2", list.get(1).getName());
		assertEquals("ibis_xml.log.-2", list.get(2).getName());
	}

} 
