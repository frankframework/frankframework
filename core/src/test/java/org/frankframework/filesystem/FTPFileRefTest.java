package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.ftp.FTPFileRef;

public class FTPFileRefTest {

	@Test
	public void testFTPFileRefSetRelative() {
		assertEquals("test123", new FTPFileRef("test123").getName());
		assertEquals("folder/test123", new FTPFileRef("folder/test123").getName());
	}

	@Test
	public void testFTPFileRefSetFolder() {
		FTPFileRef ref1 = new FTPFileRef("test123", "folder");
		assertEquals("folder/test123", ref1.getName());
	}

	@Test
	public void testFTPFileRefRelativeWithSetFolder() {
		FTPFileRef ref2 = new FTPFileRef("folder1/test123", "folder2");
		assertEquals("folder2/test123", ref2.getName());
	}

	@Test
	public void testFTPFileRefWindowsSlash() {
		FTPFileRef ref2 = new FTPFileRef("folder1\\test123", "folder2");
		assertEquals("folder2/test123", ref2.getName());
	}
}
