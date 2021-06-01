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
package nl.nn.credentialprovider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class FileSystemCredentials extends Credentials {

	private Path root;
	private String usernamefile;
	private String passwordfile;
	

	public FileSystemCredentials(String alias, String defaultUsername, String defaultPassword, Path root) {
		this(alias, defaultUsername, defaultPassword, null, null, root);
	}
	
	public FileSystemCredentials(String alias, String defaultUsername, String defaultPassword, String usernamefile, String passwordfile, Path root) {
		super(alias, defaultUsername, defaultPassword);
		this.root = root;
		this.usernamefile = StringUtils.isNotEmpty(usernamefile) ? usernamefile : FileSystemCredentialFactory.USERNAME_FILE_DEFAULT;
		this.passwordfile = StringUtils.isNotEmpty(passwordfile) ? passwordfile : FileSystemCredentialFactory.PASSWORD_FILE_DEFAULT;
	}
	
	
	private void populateFieldFromFile(String folder, String file, Consumer<String> setter) throws IOException {
		Path p = Paths.get(root.toString(), folder, file);
		System.out.println("populate from folder ["+folder+"] file ["+file+"]");
		if (Files.exists(p)) {
			String fileContents = Files.readAllLines(p).get(0);
			System.out.println("--> setting from folder ["+folder+"] file ["+file+"] value ["+fileContents+"]");
			setter.accept(fileContents);
		}
	}
	
	@Override
	protected void getCredentialsFromAlias() {
		if (StringUtils.isNotEmpty(getAlias()) && root!=null) {
			try {
				if (Files.exists(Paths.get(root.toString(), getAlias()))) {
					populateFieldFromFile(getAlias(), usernamefile, u -> setUsername(u));
					populateFieldFromFile(getAlias(), passwordfile, p -> setPassword(p));
				}
			} catch (IOException e) {
				NoSuchElementException nsee=new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]"); 
				nsee.initCause(e);
				throw nsee;
			}
		}
	}

}
