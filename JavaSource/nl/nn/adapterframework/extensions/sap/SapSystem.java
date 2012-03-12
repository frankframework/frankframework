/*
 * $Log: SapSystem.java,v $
 * Revision 1.15  2012-03-12 15:23:00  m00f069
 * Implemented logon group properties
 *
 * Revision 1.14  2012/02/06 14:33:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapSystem jco3.SapSystem} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapSystem jco2.SapSystem}.
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class SapSystem {
	private int jcoVersion = -1;
	private nl.nn.adapterframework.extensions.sap.jco3.SapSystem sapSystem3;
	private nl.nn.adapterframework.extensions.sap.jco2.SapSystem sapSystem2;

	public SapSystem() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		} else if (jcoVersion == 3) {
			sapSystem3 = new nl.nn.adapterframework.extensions.sap.jco3.SapSystem();
		} else {
			sapSystem2 = new nl.nn.adapterframework.extensions.sap.jco2.SapSystem();
		}
	}

	public void setName(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setName(string);
		} else {
			sapSystem2.setName(string);
		}
	}

	public void registerItem(Object dummyParent) {
		if (jcoVersion == 3) {
			sapSystem3.registerItem(dummyParent);
		} else {
			sapSystem2.registerItem(dummyParent);
		}
	}

	public void setHost(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setHost(string);
		}
	}

	public void setAshost(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setAshost(string);
		}
	}

	public void setSystemnr(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setSystemnr(string);
		} else {
			sapSystem2.setSystemnr(string);
		}
	}

	public void setGroup(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setGroup(string);
		}
	}

	public void setR3name(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setR3name(string);
		}
	}

	public void setMshost(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setMshost(string);
		}
	}

	public void setMsservOffset(int i) {
		if (jcoVersion == 3) {
			sapSystem3.setMsservOffset(i);
		}
	}

	public void setGwhost(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setGwhost(string);
		} else {
			sapSystem2.setGwhost(string);
		}
	}

	public void setGwservOffset(int i) {
		if (jcoVersion == 3) {
			sapSystem3.setGwservOffset(i);
		}
	}

	public void setMandant(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setMandant(string);
		} else {
			sapSystem2.setMandant(string);
		}
	}

	public void setAuthAlias(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setAuthAlias(string);
		} else {
			sapSystem2.setAuthAlias(string);
		}
	}

	public void setUserid(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setUserid(string);
		} else {
			sapSystem2.setUserid(string);
		}
	}

	public void setPasswd(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setPasswd(string);
		} else {
			sapSystem2.setPasswd(string);
		}
	}

	public void setLanguage(String string) {
		if (jcoVersion == 3) {
			sapSystem3.setLanguage(string);
		} else {
			sapSystem2.setLanguage(string);
		}
	}

	public void setUnicode(boolean b) {
		if (jcoVersion == 3) {
			sapSystem3.setUnicode(b);
		} else {
			sapSystem2.setUnicode(b);
		}
	}

	public void setMaxConnections(int i) {
		if (jcoVersion == 3) {
			sapSystem3.setMaxConnections(i);
		} else {
			sapSystem2.setMaxConnections(i);
		}
	}

	public void setTraceLevel(int i) {
		if (jcoVersion == 3) {
			sapSystem3.setTraceLevel(i);
		} else {
			sapSystem2.setTraceLevel(i);
		}
	}

	public void setServiceOffset(int i) {
		if (jcoVersion == 2) {
			sapSystem2.setServiceOffset(i);
		}
	}

}
