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

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IBasicFileSystem;

public class FileSystemCredentials<F> extends Credentials {

	public final String USERNAME_FILE_DEFAULT="username";
	public final String PASSWORD_FILE_DEFAULT="password";

	private IBasicFileSystem<F> fs;
	private String usernamefile;
	private String passwordfile;
	
	public FileSystemCredentials(String alias, String defaultUsername, String defaultPassword, IBasicFileSystem<F> fileSystem) {
		this(alias, defaultUsername, defaultPassword, null, null, fileSystem);
	}
	
	public FileSystemCredentials(String alias, String defaultUsername, String defaultPassword, String usernamefile, String passwordFile, IBasicFileSystem<F> fileSystem) {
		super(alias, defaultUsername, defaultPassword);
		fs = fileSystem;
		this.usernamefile = StringUtils.isNotEmpty(usernamefile) ? usernamefile : USERNAME_FILE_DEFAULT;
		this.passwordfile = StringUtils.isNotEmpty(passwordFile) ? usernamefile : PASSWORD_FILE_DEFAULT;
	}
	
	private void populateFieldFromFile(String folder, String file, Consumer<String> setter) throws FileSystemException, IOException {
		F f = fs.toFile(folder, file);
		if (fs.exists(f)) {
			setter.accept(fs.readFile(f).asString());
		}
	}
	
	protected void getCredentialsFromAlias() {
		if (StringUtils.isNotEmpty(getAlias()) && fs!=null) {
			try {
				if (fs.folderExists(getAlias())) {
					populateFieldFromFile(getAlias(), usernamefile, u -> setUsername(u));
					populateFieldFromFile(getAlias(), passwordfile, p -> setPassword(p));
				}
			} catch (FileSystemException | IOException e) {
				NoSuchElementException nsee=new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]"); 
				nsee.initCause(e);
				throw nsee;
			}
		}
	}

}
