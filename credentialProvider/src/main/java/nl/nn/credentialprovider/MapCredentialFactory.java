/*
   Copyright 2021 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.net.MalformedURLException;
import java.util.Map;

import nl.nn.credentialprovider.util.AppConstants;

public abstract class MapCredentialFactory implements ICredentialFactory {
	
	public final String USERNAME_SUFFIX_PROPERTY=getPropertyBase()+".usernameSuffix";
	public final String PASSWORD_SUFFIX_PROPERTY=getPropertyBase()+".passwordSuffix";
	
	public static final String USERNAME_SUFFIX_DEFAULT="/username";
	public static final String PASSWORD_SUFFIX_DEFAULT="/password";

	private String usernameSuffix;
	private String passwordSuffix;
	
	private Map<String,String> aliases;
	
	public MapCredentialFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		
		try {
			aliases = getCredentialMap(appConstants);
		} catch (Exception e) {
			throw new IllegalArgumentException(this.getClass().getName()+" cannot get alias map", e);
		}
		if (aliases == null) {
			throw new IllegalArgumentException(this.getClass().getName()+" cannot get alias map");
		}
		
		usernameSuffix = appConstants.getProperty(USERNAME_SUFFIX_PROPERTY, USERNAME_SUFFIX_DEFAULT);
		passwordSuffix = appConstants.getProperty(PASSWORD_SUFFIX_PROPERTY, PASSWORD_SUFFIX_DEFAULT);
	}

	public abstract String getPropertyBase();
	
	protected abstract Map<String,String> getCredentialMap(AppConstants appConstants) throws MalformedURLException, IOException;
	
	@Override
	public boolean hasCredentials(String alias) {
		return aliases.containsKey(alias) || aliases.containsKey(alias+usernameSuffix) || aliases.containsKey(alias+passwordSuffix);
	}

	@Override
	public ICredentials getCredentials(String alias, String defaultUsername, String defaultPassword) {
		return new MapCredentials(alias, defaultUsername, defaultPassword, usernameSuffix, passwordSuffix, aliases);
	}

}