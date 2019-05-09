/*
   Copyright 2013, 2019 Nationale-Nederlanden

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
import nl.nn.adapterframework.extensions.sap.jco3.ISapSystem3;
import nl.nn.adapterframework.extensions.sap.jco3.ISncEnabled;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapSystem jco3.SapSystem} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapSystem jco2.SapSystem}.
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @author  Niels Meijer
 * @since   5.0
 */
public class SapSystem {
	private int jcoVersion = -1;
	private ISapSystem sapSystem;

	public SapSystem() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		}
		try {
			Class<?> clazz = ClassUtils.loadClass("nl.nn.adapterframework.extensions.sap.jco"+jcoVersion+".SapSystem");
			Constructor<?> con = clazz.getConstructor();
			sapSystem = (ISapSystem) con.newInstance();
		}
		catch (Exception e) {
			throw new ConfigurationException("failed to load SapSystem version ["+jcoVersion+"]", e);
		}
	}

	public void setName(String string) {
		sapSystem.setName(string);
	}

	public void registerItem(Object dummyParent) {
		sapSystem.registerItem(dummyParent);
		SapSystemFactory.getInstance().registerSapSystem(sapSystem, sapSystem.getName());
	}

	public void setHost(String string) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setHost(string);
		}
	}

	public void setAshost(String string) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setAshost(string);
		}
	}

	public void setSystemnr(String string) {
		sapSystem.setSystemnr(string);
	}

	public void setGroup(String string) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setGroup(string);
		}
	}

	public void setR3name(String string) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setR3name(string);
		}
	}

	public void setMshost(String string) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setMshost(string);
		}
	}

	public void setMsservOffset(int i) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setMsservOffset(i);
		}
	}

	public void setGwhost(String string) {
		sapSystem.setGwhost(string);
	}

	public void setGwservOffset(int i) {
		if(sapSystem instanceof ISapSystem3) {
			((ISapSystem3) sapSystem).setGwservOffset(i);
		}
	}

	public void setMandant(String string) {
		sapSystem.setMandant(string);
	}

	public void setAuthAlias(String string) {
		sapSystem.setAuthAlias(string);
	}

	public void setUserid(String string) {
		sapSystem.setUserid(string);
	}

	public void setPasswd(String string) {
		sapSystem.setPasswd(string);
	}

	public void setLanguage(String string) {
		sapSystem.setLanguage(string);
	}

	public void setUnicode(boolean b) {
		sapSystem.setUnicode(b);
	}

	public void setMaxConnections(int i) {
		sapSystem.setMaxConnections(i);
	}

	public void setTraceLevel(int i) {
		sapSystem.setTraceLevel(i);
	}

	public void setServiceOffset(int i) {
		sapSystem.setServiceOffset(i);
	}

	public void setSncEnabled(boolean sncEnabled) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setSncEnabled(sncEnabled);
		}
	}

	public void setSncLibrary(String sncLibPath) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setSncLibrary(sncLibPath);
		}
	}

	public void setSncQop(int qop) throws ConfigurationException {
		if (sapSystem instanceof ISncEnabled) {
			if(qop < 1 || qop > 9)
				throw new ConfigurationException("SNC QOP cannot be smaller then 0 or larger then 9");
			((ISncEnabled) sapSystem).setSncQop(qop);
		}
	}

	public void setMyName(String myName) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setMyName(myName);
		}
	}

	public void setPartnerName(String partnerName) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setPartnerName(partnerName);
		}
	}

	public void setSncAuthMethod(String sncAuthMethod) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setSncAuthMethod(sncAuthMethod);
		}
	}

	public void setSncSSO2(String sncSSO2) {
		if (sapSystem instanceof ISncEnabled) {
			((ISncEnabled) sapSystem).setSncSSO2(sncSSO2);
		}
	}

}
