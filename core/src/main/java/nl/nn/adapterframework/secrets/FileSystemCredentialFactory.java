/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.secrets;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.filesystem.IBasicFileSystem;
import nl.nn.adapterframework.filesystem.LocalFileSystem;
import nl.nn.adapterframework.util.AppConstants;

public class FileSystemCredentialFactory implements ICredentialFactory {

	public final String FILESYSTEM_ROOT_PROPERTY="credentialFactory.filesystem.root";
	public final String USERNAME_FILE_PROPERTY="credentialFactory.filesystem.usernamefile";
	public final String PASSWORD_FILE_PROPERTY="credentialFactory.filesystem.passwordfile";
	
	private IBasicFileSystem fs;
	private String usernamefile;
	private String passwordfile;
	
	public FileSystemCredentialFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		String root = appConstants.getProperty(FILESYSTEM_ROOT_PROPERTY);
		if (StringUtils.isEmpty(root)) {
			throw new IllegalStateException("No property ["+FILESYSTEM_ROOT_PROPERTY+"] found for credentialFactory ["+FileSystemCredentialFactory.class.getTypeName()+"]");
		}
		LocalFileSystem lfs = new LocalFileSystem();
		lfs.setRoot(root);
		fs = lfs;
		
		usernamefile = appConstants.getProperty(USERNAME_FILE_PROPERTY);
		passwordfile = appConstants.getProperty(PASSWORD_FILE_PROPERTY);
	}
	
	@Override
	public ICredentials getCredentials(String alias, String defaultUsername, String defaultPassword) {
		return new FileSystemCredentials(alias, defaultUsername, defaultPassword, usernamefile, passwordfile, fs);
	}

}