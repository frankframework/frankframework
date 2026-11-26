/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.javascript;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

/**
 * Clears the 'executable' flag of the {@code PT_GNU_STACK} program header of an ELF shared library.
 * <p>
 * The native J2V8 library is linked without {@code -z noexecstack}, and as such requests an executable stack.
 * Since glibc 2.41 (Ubuntu 25.04 and up) the dynamic loader refuses to load such a library.
 * Older versions of glibc made the stack of the entire JVM executable, which is a security risk.
 * This does the same as {@code execstack -c} and {@code patchelf --clear-execstack}, without having to rely on those tools being installed.
 * </p>
 */
final class ElfExecutableStackCleaner {
	private static final Logger LOG = LogUtil.getLogger(ElfExecutableStackCleaner.class);

	private static final int ELF_MAGIC = 0x7F454C46; // 0x7F 'E' 'L' 'F'
	private static final byte ELFCLASS32 = 1;
	private static final byte ELFCLASS64 = 2;
	private static final byte ELFDATA2LSB = 1;
	private static final byte ELFDATA2MSB = 2;
	private static final int PT_GNU_STACK = 0x6474E551;
	private static final int PF_X = 1;

	private ElfExecutableStackCleaner() {
		// Utility class
	}

	/**
	 * Clears the executable stack flag when present. Files that are not ELF files are left untouched.
	 * Never fails: if the file cannot be patched the caller will find out when attempting to load the library.
	 *
	 * @return {@code true} when the file has been modified
	 */
	static boolean clearExecutableStackFlag(Path file) {
		try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			return clearExecutableStackFlag(channel);
		} catch (IOException | RuntimeException e) {
			LOG.warn("unable to clear the executable stack flag of [{}]", file, e);
			return false;
		}
	}

	private static boolean clearExecutableStackFlag(FileChannel channel) throws IOException {
		ByteBuffer header = ByteBuffer.allocate(64);
		if (!readFully(channel, header, 0) || header.getInt(0) != ELF_MAGIC) {
			return false;
		}

		boolean is64Bit = switch (header.get(4)) {
			case ELFCLASS32 -> false;
			case ELFCLASS64 -> true;
			default -> throw new IOException("unknown ELF class");
		};
		ByteOrder order = switch (header.get(5)) {
			case ELFDATA2LSB -> ByteOrder.LITTLE_ENDIAN;
			case ELFDATA2MSB -> ByteOrder.BIG_ENDIAN;
			default -> throw new IOException("unknown ELF byte order");
		};
		header.order(order);

		long programHeaderOffset = is64Bit ? header.getLong(0x20) : Integer.toUnsignedLong(header.getInt(0x1C));
		int programHeaderSize = Short.toUnsignedInt(header.getShort(is64Bit ? 0x36 : 0x2A));
		int programHeaderCount = Short.toUnsignedInt(header.getShort(is64Bit ? 0x38 : 0x2C));
		int flagsOffset = is64Bit ? 4 : 24; // p_flags is located after p_type, or after p_type, p_offset, p_vaddr, p_paddr, p_filesz and p_memsz

		boolean modified = false;
		ByteBuffer flags = ByteBuffer.allocate(Integer.BYTES).order(order);
		for (int i = 0; i < programHeaderCount; i++) {
			long entryPosition = programHeaderOffset + (long) i * programHeaderSize;

			ByteBuffer type = ByteBuffer.allocate(Integer.BYTES).order(order);
			if (!readFully(channel, type, entryPosition) || type.getInt(0) != PT_GNU_STACK) {
				continue;
			}

			flags.clear();
			if (readFully(channel, flags, entryPosition + flagsOffset) && (flags.getInt(0) & PF_X) != 0) {
				flags.putInt(0, flags.getInt(0) & ~PF_X);
				flags.rewind();
				channel.write(flags, entryPosition + flagsOffset);
				modified = true;
			}
		}
		return modified;
	}

	private static boolean readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
		long current = position;
		while (buffer.hasRemaining()) {
			int read = channel.read(buffer, current);
			if (read < 0) {
				return false;
			}
			current += read;
		}
		buffer.flip();
		return true;
	}
}
