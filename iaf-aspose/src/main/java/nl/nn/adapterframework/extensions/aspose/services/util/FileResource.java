package nl.nn.adapterframework.extensions.aspose.services.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import com.google.common.base.MoreObjects;

/**
 * Deze klasse maakt het mogelijk om een file te delen tussen verschillende
 * objecten en gedelete als het laatste object wordt gedestroyed.
 * <p/>
 * Indien een nieuwe object een nieuwe reference naar de file wil hebben dan
 * moet deze via {@link #copy()} opgevraagd worden. <br/>
 * Als de reference opgeruimd wordt dan moet een {@link #destroy()} worden
 * aangeroepen.
 * 
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 */
public class FileResource {

	private static final Logger LOGGER = Logger.getLogger(FileResource.class);

	private FileResourceCount fileResourceCount;

	public FileResource(File file) {
		fileResourceCount = new FileResourceCount(file);
	}

	private FileResource(FileResource fileResource) {
		fileResourceCount = fileResource.fileResourceCount.copy();
	}

	public FileResource copy() {
		checkState();
		return new FileResource(this);
	}

	public File getFile() {
		checkState();
		return fileResourceCount.getFile();
	}

	public void destroy() {
		checkState();
		fileResourceCount.destroy();
		fileResourceCount = null;
	}

	private void checkState() {
		if (isDestroyed()) {
			throw new IllegalStateException("Resource is already destroyed");
		}
	}

	public boolean isDestroyed() {
		return fileResourceCount == null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("FileResourceCount", fileResourceCount).toString();
	}

	/**
	 * Klasse die file en reference counter bijhoudt voor elke gedeelde file.
	 */
	class FileResourceCount {

		private File file;

		private int referenceCount = 0;

		FileResourceCount(File file) {
			this.file = file;
			referenceCount++;
		}

		FileResourceCount copy() {
			referenceCount++;
			return this;
		}

		File getFile() {
			return file;
		}

		@SuppressWarnings("synthetic-access")
		void destroy() {

			referenceCount--;
			if (referenceCount <= 0) {

				// delete the file.
				try {
					LOGGER.debug("Delete file " + getFile());
					if (getFile().exists()) {
						Files.delete(getFile().toPath());
					} else {
						LOGGER.warn("File to delete " + getFile() + " does not exists anymore.");
					}
				} catch (IOException e) {
					LOGGER.error("Failed to delete the file: " + getFile(), e);
				}
			}
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("file", file).add("referenceCount", referenceCount).toString();
		}
	}
}
