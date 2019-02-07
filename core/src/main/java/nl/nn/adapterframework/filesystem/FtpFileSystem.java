/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * 
 * @author Daniël Meyer
 *
 */
public class FtpFileSystem implements IFileSystem<FTPFile> {

	private FtpSession ftpSession;
	
	private String remoteDirectory;
	private String remoteFilenamePattern=null;

	private class FTPFilePathIterator implements Iterator<FTPFile> {

		private FTPFile files[];
		int i=0;
		
		FTPFilePathIterator(FTPFile files[]) {
			Vector<FTPFile> fList = new Vector<FTPFile>();
			for(int i = 0; i < files.length; i++) {
				String filename = files[i].getName();
				if(!filename.equals(".") && !filename.equals("..")) {
					fList.addElement(files[i]);
				}
			}
			
			this.files = new FTPFile[fList.size()];
			fList.copyInto(this.files);
		}
		
		@Override
		public boolean hasNext() {
			return files!=null && i<files.length;
		}

		@Override
		public FTPFile next() {			
			return files[i++];
		}

		@Override
		public void remove() {
			try {
				deleteFile(files[i++]);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		}
	}
	
	public FtpFileSystem() {
		ftpSession = new FtpSession();
	}
	
	@Override
	public void configure() throws ConfigurationException {
		ftpSession.configure();

		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			e.printStackTrace();
		}
	}

	@Override
	public FTPFile toFile(String filename) throws FileSystemException {
		FTPFile ftpFile;
		ftpFile = new FTPFile();
		ftpFile.setName(filename);
		
		return ftpFile;
	}

	@Override
	public Iterator<FTPFile> listFiles() throws FileSystemException {
		try {
			return new FTPFilePathIterator(ftpSession.ftpClient.listFiles(remoteDirectory));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for(FTPFile o : files) {
				if(o.getName().equals(f.getName())) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public OutputStream createFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.storeFileStream(f.getName());
	}

	@Override
	public OutputStream appendFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.appendFileStream(f.getName());
	}

	@Override
	public InputStream readFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.retrieveFileStream(f.getName());
	}
	
	@Override
	public void deleteFile(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.deleteFile(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getInfo(FTPFile f) throws FileSystemException {
		return getFileAsXmlBuilder(f).toXML();
	}

	@Override
	public boolean isFolder(FTPFile f) throws FileSystemException {
		return f.isDirectory();
	}

	@Override
	public void createFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.makeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.removeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void renameTo(FTPFile f, String destination) throws FileSystemException {
		try {
			ftpSession.ftpClient.rename(f.getName(), destination);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public XmlBuilder getFileAsXmlBuilder(FTPFile f) throws FileSystemException {
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("name", f.getName());
		fileXml.addAttribute("user", f.getUser());
		fileXml.addAttribute("group", f.getGroup());
		fileXml.addAttribute("type", f.getType());
		fileXml.addAttribute("size", "" + f.getSize());
		fileXml.addAttribute("rawListing", f.getRawListing());
		fileXml.addAttribute("isDirectory", "" + isFolder(f));
		fileXml.addAttribute("link", f.getLink());
		fileXml.addAttribute("hardLinkCount", f.getHardLinkCount());

		if(f.getTimestamp() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			Date date = new Date(f.getTimestamp().getTimeInMillis());
			fileXml.addAttribute("modificationDate", sdf.format(date));
			
			sdf = new SimpleDateFormat("hh:mm:ss");
			date = new Date(f.getTimestamp().getTimeInMillis());
			fileXml.addAttribute("modificationTime", sdf.format(date));
		}
		
		return fileXml;
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, FTPFile f) {
		dirInfo.addAttribute("name", f.getName());
	}
	
	public FtpSession getFtpSession() {
		return ftpSession;
	}
	
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteFilenamePattern(String string) {
		this.remoteFilenamePattern = string;
	}
	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
	}

	@Override
	public void finalizeAction() {
		try {
			ftpSession.ftpClient.completePendingCommand();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}