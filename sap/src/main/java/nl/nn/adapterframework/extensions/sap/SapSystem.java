/*
   Copyright 2013,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap;

import java.lang.reflect.Constructor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapSystem jco3.SapSystem} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapSystem jco2.SapSystem}.
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author Jaco de Groot
 * @author Niels Meijer
 * @author Gerrit van Brakel
 * @since  5.0
 */
public class SapSystem {

	private ISapSystem delegate;

	public SapSystem() throws ConfigurationException {
		int jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		}

		try {
			Class<?> clazz = ClassUtils.loadClass("nl.nn.adapterframework.extensions.sap.jco"+jcoVersion+".SapSystem");
			Constructor<?> con = clazz.getConstructor();
			delegate = (ISapSystem) con.newInstance();
		}
		catch (Exception e) {
			throw new ConfigurationException("failed to load SapSystem version ["+jcoVersion+"]", e);
		}
	}

	public void setName(String string) {
		delegate.setName(string);
	}

	public void registerItem(Object dummyParent) {
		delegate.registerItem(dummyParent);
		SapSystemFactory.getInstance().registerSapSystem(delegate, delegate.getName());
	}

	public void setHost(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setHost(string);
		}
	}

	public void setAshost(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setAshost(string);
		}
	}

	public void setSystemnr(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setSystemnr(string);
		}
	}

	public void setGroup(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setGroup(string);
		}
	}

	public void setR3name(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setR3name(string);
		}
	}

	public void setMshost(String string) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setMshost(string);
		}
	}

	public void setMsservOffset(int i) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setMsservOffset(i);
		}
	}

	public void setGwhost(String string) {
		delegate.setGwhost(string);
	}

	public void setGwservOffset(int i) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setGwservOffset(i);
		}
	}

	public void setMandant(String string) {
		delegate.setMandant(string);
	}

	public void setAuthAlias(String string) {
		delegate.setAuthAlias(string);
	}

	public void setUserid(String string) {
		delegate.setUserid(string);
	}

	public void setPasswd(String string) {
		delegate.setPasswd(string);
	}

	public void setLanguage(String string) {
		delegate.setLanguage(string);
	}

	public void setUnicode(boolean b) {
		delegate.setUnicode(b);
	}

	public void setMaxConnections(int i) {
		delegate.setMaxConnections(i);
	}

	public void setTraceLevel(int i) {
		delegate.setTraceLevel(i);
	}

	public void setServiceOffset(int i) {
		delegate.setServiceOffset(i);
	}

	public void setSncEnabled(boolean sncEnabled) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setSncEnabled(sncEnabled);
		}
	}

	public void setSncLibrary(String sncLibPath) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setSncLibrary(sncLibPath);
		}
	}

	public void setSncQop(int qop) throws ConfigurationException {
		if (delegate instanceof ISapSystemJco3) {
			if(qop < 1 || qop > 9)
				throw new ConfigurationException("SNC QOP cannot be smaller then 0 or larger then 9");
			((ISapSystemJco3)delegate).setSncQop(qop);
		}
	}

	public void setMyName(String myName) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setMyName(myName);
		}
	}

	public void setPartnerName(String partnerName) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setPartnerName(partnerName);
		}
	}

	public void setSncAuthMethod(String sncAuthMethod) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setSncAuthMethod(sncAuthMethod);
		}
	}

	public void setSncSSO2(String sncSSO2) {
		if (delegate instanceof ISapSystemJco3) {
			((ISapSystemJco3)delegate).setSncSSO2(sncSSO2);
		}
	}

}
