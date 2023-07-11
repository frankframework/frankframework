package nl.nn.adapterframework.filesystem.samba;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.filesystem.smb.SmbFileRef;

public class SmbFileRefTest {

	@Test
	public void testSmbFileRefSetRelative() {
		assertEquals("test123", new SmbFileRef("test123").getName());
		assertEquals("folder\\test123", new SmbFileRef("folder/test123").getName());
	}

	@Test
	public void testSmbFileRefSetFolder() {
		SmbFileRef ref1 = new SmbFileRef("test123");
		ref1.setFolder("folder");
		assertEquals("folder\\test123", ref1.getName());
	}

	@Test
	public void testSmbFileRefRelativeWithSetFolder() {
		SmbFileRef ref2 = new SmbFileRef("folder1/test123");
		ref2.setFolder("folder2");
		assertEquals("folder2\\test123", ref2.getName());
	}

	@Test
	public void testSmbFileRefWindowsSlash() {
		SmbFileRef ref2 = new SmbFileRef("folder1\\test123");
		ref2.setFolder("folder2");
		assertEquals("folder2\\test123", ref2.getName());
	}
}
