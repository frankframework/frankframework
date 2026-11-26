package org.frankframework.javascript;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ElfExecutableStackCleanerTest {
	private static final int PT_GNU_STACK = 0x6474E551;
	private static final int PF_X = 1;

	@TempDir
	private Path tempDir;

	@Test
	void clearsExecutableStackFlagOfJ2V8Library() throws Exception {
		Path library = tempDir.resolve("libj2v8_linux_x86_64.so");
		try (InputStream is = getClass().getResourceAsStream("/libj2v8_linux_x86_64.so")) {
			assertNotNull(is);
			Files.copy(is, library);
		}
		byte[] original = Files.readAllBytes(library);
		assertTrue((getGnuStackFlags(original) & PF_X) != 0, "J2V8 library is expected to request an executable stack");

		assertTrue(ElfExecutableStackCleaner.clearExecutableStackFlag(library));

		byte[] patched = Files.readAllBytes(library);
		assertEquals(0, getGnuStackFlags(patched) & PF_X);
		assertEquals(original.length, patched.length);
		assertEquals(1, countDifferingBytes(original, patched));

		assertFalse(ElfExecutableStackCleaner.clearExecutableStackFlag(library), "already cleared");
	}

	@Test
	void leavesNonElfFilesUntouched() throws Exception {
		Path file = tempDir.resolve("not-an-elf.so");
		byte[] content = "this is not an ELF file, but it is longer than 64 bytes so that the header can be read".getBytes();
		Files.write(file, content);

		assertFalse(ElfExecutableStackCleaner.clearExecutableStackFlag(file));

		assertArrayEquals(content, Files.readAllBytes(file));
	}

	@Test
	void doesNotFailOnMissingFile() {
		assertFalse(ElfExecutableStackCleaner.clearExecutableStackFlag(tempDir.resolve("missing.so")));
	}

	private static int countDifferingBytes(byte[] a, byte[] b) {
		int differences = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				differences++;
			}
		}
		return differences;
	}

	// Minimal independent reader for little-endian ELF64 files.
	private static int getGnuStackFlags(byte[] elf) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN);
		long programHeaderOffset = buffer.getLong(0x20);
		int programHeaderSize = buffer.getShort(0x36);
		int programHeaderCount = buffer.getShort(0x38);
		for (int i = 0; i < programHeaderCount; i++) {
			int entry = (int) programHeaderOffset + i * programHeaderSize;
			if (buffer.getInt(entry) == PT_GNU_STACK) {
				return buffer.getInt(entry + 4);
			}
		}
		throw new IOException("no PT_GNU_STACK header found");
	}
}
