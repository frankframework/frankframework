/*
   Copyright 2020-2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import lombok.Lombok;

import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;
import org.frankframework.util.TimeProvider;
import org.frankframework.util.UUIDUtil;
import org.frankframework.util.WildCardFilter;

public class FileSystemUtils {
	protected static Logger log = LogUtil.getLogger(FileSystemUtils.class);
	private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

	private FileSystemUtils() {
		// Private constructor so that the utility-class cannot be instantiated.
	}

	/**
	 * Check if a source file exists.
	 */
	public static <F> void checkSource(IBasicFileSystem<F> fileSystem, F source, FileSystemAction action) throws FileSystemException {
		if (!fileSystem.exists(source)) {
			throw new FileNotFoundException("file to " + action.getLabel() + " [" + fileSystem.getName(source) + "], canonical name [" + fileSystem.getCanonicalNameOrErrorMessage(source) + "], does not exist");
		}
	}

	/**
	 * Prepares the destination of a file:
	 * - if the file exists, checks overwrite, or performs rollover
	 */
	public static <F> void prepareDestination(IWritableFileSystem<F> fileSystem, F destination, boolean overwrite, int numOfBackups, FileSystemAction action) throws FileSystemException {
		if (fileSystem.exists(destination)) {
			if (overwrite) {
				log.debug("removing current destination file [{}]", () -> fileSystem.getCanonicalNameOrErrorMessage(destination));
				fileSystem.deleteFile(destination);
			} else {
				if (numOfBackups > 0) {
					FileSystemUtils.rolloverByNumber(fileSystem, destination, numOfBackups);
				} else {
					throw new FileAlreadyExistsException("Cannot " + action.getLabel() + " file to [" + fileSystem.getName(destination) + "]. Destination file [" + fileSystem.getCanonicalNameOrErrorMessage(destination) + "] already exists.");
				}
			}
		}
	}

	/**
	 * Prepares the destination folder, e.g. for move or copy.
	 */
	public static <F> void prepareDestination(IBasicFileSystem<F> fileSystem, F source, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders, FileSystemAction action) throws FileSystemException {
		if (!fileSystem.folderExists(destinationFolder)) {
			if (fileSystem.exists(fileSystem.toFile(destinationFolder))) {
				throw new FileAlreadyExistsException("destination [" + destinationFolder + "] exists but is not a folder");
			}
			if (createFolders) {
				fileSystem.createFolder(destinationFolder);
			} else {
				throw new FolderNotFoundException("destination folder [" + destinationFolder + "] does not exist");
			}
		}
		if (fileSystem instanceof IWritableFileSystem) {
			F destinationFile = fileSystem.toFile(destinationFolder, fileSystem.getName(source));
			prepareDestination((IWritableFileSystem<F>) fileSystem, destinationFile, overwrite, numOfBackups, action);
		}
	}

	public static <F> F renameFile(IWritableFileSystem<F> fileSystem, F source, F destination, boolean overwrite, int numOfBackups) throws FileSystemException {
		checkSource(fileSystem, source, FileSystemAction.RENAME);
		prepareDestination(fileSystem, destination, overwrite, numOfBackups, FileSystemAction.RENAME);
		F newFile = fileSystem.renameFile(source, destination);
		if (newFile == null) {
			throw new FileSystemException("cannot rename file [" + fileSystem.getName(source) + "] to [" + fileSystem.getName(destination) + "]");
		}
		return newFile;
	}

	public static <F> F moveFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders, boolean destinationMustBeReturned) throws FileSystemException {
		return moveFile(fileSystem, file, destinationFolder, null, overwrite, numOfBackups, createFolders, destinationMustBeReturned);
	}

	public static <F> F moveFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, String comment, boolean overwrite, int numOfBackups, boolean createFolders, boolean destinationMustBeReturned) throws FileSystemException {
		checkSource(fileSystem, file, FileSystemAction.MOVE);
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders, FileSystemAction.MOVE);
		F newFile;
		if (StringUtils.isNotEmpty(comment) && fileSystem instanceof ISupportsCustomFileAttributes<?>) {
			@SuppressWarnings("unchecked")
			ISupportsCustomFileAttributes<F> fsWithAttributes = (ISupportsCustomFileAttributes<F>) fileSystem;
			newFile = fsWithAttributes.moveFile(file, destinationFolder, createFolders, Map.of("comment", comment));
		} else {
			newFile = fileSystem.moveFile(file, destinationFolder, createFolders);
		}
		if (newFile == null && destinationMustBeReturned) {
			throw new FileSystemException("cannot move file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}

	public static <F> F copyFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders, boolean destinationMustBeReturned) throws FileSystemException {
		checkSource(fileSystem, file, FileSystemAction.COPY);
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders, FileSystemAction.COPY);
		F newFile = fileSystem.copyFile(file, destinationFolder, createFolders);
		if (newFile == null && destinationMustBeReturned) {
			throw new FileSystemException("cannot copy file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}

	public static <F> MessageContext getContext(IBasicFileSystem<F> fileSystem, F file) throws FileSystemException {
		MessageContext context = new MessageContext()
				.withName(fileSystem.getName(file))
				.withLocation(fileSystem.getCanonicalName(file))
				.withModificationTime(fileSystem.getModificationTime(file))
				.withSize(fileSystem.getFileSize(file));
		Map<String, Object> fileProperties = fileSystem.getAdditionalFileProperties(file);
		if (fileProperties != null) {
			fileProperties.forEach((key, value) -> {
				if (value instanceof Serializable s) {
					context.put(key, s);
				} else if (value != null) {
					context.put(key, value.toString());
				}
			});
		}
		return context;
	}

	public static <F> MessageContext getContext(IBasicFileSystem<F> fileSystem, F file, String charset) throws FileSystemException {
		return getContext(fileSystem, file).withCharset(charset);
	}

	public static <F> void rolloverByNumber(IWritableFileSystem<F> fileSystem, F file, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		String filename = fileSystem.getCanonicalName(file);

		String tmpFilename = filename + ".tmp-" + UUIDUtil.createUUID();
		F tmpFile = fileSystem.toFile(tmpFilename);
		tmpFile = fileSystem.renameFile(file, tmpFile);

		log.debug("Rotating files with a name starting with [{}] and keeping [{}] backups", filename, numberOfBackups);
		F lastFile = fileSystem.toFile(filename + "." + numberOfBackups);
		if (fileSystem.exists(lastFile)) {
			log.debug("deleting file [{}.{}]", filename, numberOfBackups);
			fileSystem.deleteFile(lastFile);
		}

		for (int i = numberOfBackups - 1; i > 0; i--) {
			String sourceFilename = filename + "." + i;
			String destinationFilename = filename + "." + (i + 1);
			F source = fileSystem.toFile(sourceFilename);
			F destination = fileSystem.toFile(destinationFilename);

			if (fileSystem.exists(source)) {
				log.debug("moving file [{}] to file [{}]", sourceFilename, destinationFilename);
				fileSystem.renameFile(source, destination);
			} else {
				log.debug("file [{}] does not exist, no need to move", sourceFilename);
			}
		}
		String destinationFilename = filename + ".1";
		F destination = fileSystem.toFile(destinationFilename);
		log.debug("moving file [{}] to file [{}]", tmpFilename, destinationFilename);
		fileSystem.renameFile(tmpFile, destination);
	}


	public static <F> void rolloverBySize(IWritableFileSystem<F> fileSystem, F file, int rotateSize, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		if (fileSystem.getFileSize(file) > rotateSize) {
			rolloverByNumber(fileSystem, file, numberOfBackups);
		}
	}

	@Nonnull
	public static <F> DirectoryStream<F> getDirectoryStream(Iterable<F> iterable) {

		return new DirectoryStream<>() {

			@Override
			public void close() throws IOException {
				if (iterable instanceof AutoCloseable closeable) {
					try {
						closeable.close();
					} catch (IOException e) {
						throw e;
					} catch (Exception e) {
						throw new IOException(e);
					}
				}
			}

			@Override
			public Iterator<F> iterator() {
				return iterable.iterator();
			}

		};
	}

	@Nonnull
	public static <F> DirectoryStream<F> getDirectoryStream(@Nullable Iterator<F> iterator) {
		return getDirectoryStream(iterator, null);
	}

	@Nonnull
	public static <F> DirectoryStream<F> getDirectoryStream(@Nullable Iterator<F> iterator, @Nullable Supplier<IOException> onClose) {

		return new DirectoryStream<>() {

			@Override
			public void close() throws IOException {
				if (onClose != null) {
					IOException result = onClose.get();
					if (result != null) {
						throw result;
					}
				}
			}

			@Override
			public Iterator<F> iterator() {
				return iterator == null ? Collections.emptyIterator() : iterator;
			}

		};
	}

	public static <F> void rolloverByDay(IWritableFileSystem<F> fileSystem, F file, String folder, int rotateDays) throws FileSystemException {
		Date lastModified = fileSystem.getModificationTime(file);
		Date sysTime = TimeProvider.nowAsDate();
		if (DateUtils.isSameDay(lastModified, sysTime) || lastModified.after(sysTime)) {
			return;
		}
		String srcFilename = fileSystem.getCanonicalName(file);
		F tgtFilename = fileSystem.toFile(srcFilename + "." + DateFormatUtils.format(lastModified, DateFormatUtils.ISO_DATE_FORMATTER));
		fileSystem.renameFile(file, tgtFilename);

		log.debug("Deleting files in folder [{}] that have a name starting with [{}] and are older than [{}] days", folder, srcFilename, rotateDays);
		long threshold = sysTime.getTime() - rotateDays * MILLIS_PER_DAY;
		try (DirectoryStream<F> ds = fileSystem.list(folder, TypeFilter.FILES_ONLY)) {
			for (F f : ds) {
				String filename = fileSystem.getName(f);
				if (filename != null && filename.startsWith(srcFilename) && fileSystem.getModificationTime(f).getTime() < threshold) {
					log.debug("deleting file [{}]", filename);
					fileSystem.deleteFile(f);
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Nonnull
	public static <F> Stream<F> getFilteredStream(IBasicFileSystem<F> fileSystem, String folder, String wildCard, String excludeWildCard, @Nonnull TypeFilter typeFilter) throws FileSystemException {
		DirectoryStream<F> ds = fileSystem.list(folder, typeFilter);
		if (ds == null) {
			return Stream.empty();
		}
		Iterator<F> it = ds.iterator();

		WildCardFilter wildcardfilter = StringUtils.isEmpty(wildCard) ? null : new WildCardFilter(wildCard);
		WildCardFilter excludeFilter = StringUtils.isEmpty(excludeWildCard) ? null : new WildCardFilter(excludeWildCard);

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false)
				.filter(f -> (wildcardfilter == null || wildcardfilter.accept(null, fileSystem.getName(f)))
						&& (excludeFilter == null || !excludeFilter.accept(null, fileSystem.getName(f))))
				.onClose(() -> {
					try {
						ds.close();
					} catch (IOException e) {
						throw Lombok.sneakyThrow(e);
					}
				});
	}


	public static <F, FS extends IBasicFileSystem<F>> String getFileInfo(FS fileSystem, F f, DocumentFormat format) throws FileSystemException {
		StringWriter writer = new StringWriter();
		try (StringWriter w = writer; INodeBuilder builder = DocumentBuilderFactory.startDocument(format, "file", w)) {
			getFileInfo(fileSystem, f, builder);
		} catch (IOException | SAXException e) {
			throw new FileSystemException("Cannot get FileInfo", e);
		}
		return writer.toString();
	}

	public static <F, FS extends IBasicFileSystem<F>> void getFileInfo(FS fileSystem, F f, INodeBuilder nodeBuilder) throws FileSystemException, SAXException {
		try (ObjectBuilder file = nodeBuilder.startObject()) {
			String name = fileSystem.getName(f);
			file.addAttribute("name", name);
			if (!".".equals(name) && !"..".equals(name)) {
				long fileSize = fileSystem.getFileSize(f);
				file.addAttribute("size", "" + fileSize);
				file.addAttribute("fSize", Misc.toFileSize(fileSize, true));
				try {
					file.addAttribute("canonicalName", fileSystem.getCanonicalName(f));
				} catch (Exception e) {
					log.warn("cannot get canonicalName for file [{}]", name, e);
					file.addAttribute("canonicalName", name);
				}
				// Add type of item: file or folder
				file.addAttribute("type", fileSystem.isFolder(f) ? "folder" : "file");

				// Get the modification date of the file
				Date modificationDate = fileSystem.getModificationTime(f);
				//add date
				if (modificationDate != null) {
					String date = DateFormatUtils.format(modificationDate, DateFormatUtils.ISO_DATE_FORMATTER);
					file.addAttribute("modificationDate", date);

					// add the time
					String time = DateFormatUtils.format(modificationDate, DateFormatUtils.TIME_HMS_FORMATTER);
					file.addAttribute("modificationTime", time);
				}
			}

			Map<String, Object> additionalParameters = fileSystem.getAdditionalFileProperties(f);
			if (additionalParameters != null) {
				for (Map.Entry<String, Object> attribute : additionalParameters.entrySet()) {
					file.addAttribute(attribute.getKey(), String.valueOf(attribute.getValue()));
				}
			}
		}
	}
}
