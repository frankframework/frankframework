package nl.nn.adapterframework.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * FileNameComparator Tester.
 *
 * @author <Sina Sen>
 */
public class FileNameComparatorTest {


	/**
	 * Method: getNextIndex(String s, int start, boolean numericPart)
	 */
	@Test
	public void testGetNextIndex() throws Exception {
		int i = FileNameComparator.getNextIndex("test.txt", 0, true);
		int j = FileNameComparator.getNextIndex("test.txt", 0, false);
		assertEquals(0, i);
		assertEquals(8, j);
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
		String s3 = "file1.txt";
		String s4 = "File2.txt";
		int a = FileNameComparator.compareStringsNaturalOrder(s1, s2, true);
		int b = FileNameComparator.compareStringsNaturalOrder(s1, s3, true);
		int c = FileNameComparator.compareStringsNaturalOrder(s2, s3, true);
		int d = FileNameComparator.compareStringsNaturalOrder(s2, s4, true);
		assertEquals(32, a);
		assertEquals(0, b);
		assertEquals(-32, c);
		assertEquals(0, d);
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
		int i = FileNameComparator.compareFilenames(f1, f2);
		int j = FileNameComparator.compareFilenames(f1, f3);
		int k = FileNameComparator.compareFilenames(f1, f4);
		int l = FileNameComparator.compareFilenames(f2, f1);
		assertEquals(-13, i);
		assertEquals(0, j);
		assertEquals(32, k);
		assertEquals(13, l);
	}

} 
