package org.frankframework.jta.xa;

import javax.sql.XADataSource;

public class XaDataSourceModifier {
	private static XaResourceObserverFactory xaResourceObserverFactory;

	private XaDataSourceModifier() {
		// Private constructor to prevent creating instances
	}

	/**
	 * Register a new factory, if no other factory has already been registered.
	 */
	public static synchronized void registerFactory(XaResourceObserverFactory xaResourceObserverFactory) {
		if (XaDataSourceModifier.xaResourceObserverFactory != null) {
			throw new IllegalStateException("Another XaResourceObserverFactory is already registered");
		}
		XaDataSourceModifier.xaResourceObserverFactory = xaResourceObserverFactory;
	}

	public static <T extends XaResourceObserverFactory> T getXaResourceObserverFactory() {
		//noinspection unchecked
		return (T)XaDataSourceModifier.xaResourceObserverFactory;
	}

	public static synchronized void removeFactory() {
		if (XaDataSourceModifier.xaResourceObserverFactory != null) {
			XaDataSourceModifier.xaResourceObserverFactory.destroy();
		}
		XaDataSourceModifier.xaResourceObserverFactory = null;
	}

	public static XADataSource augmentXADataSource(XADataSource xaDataSource) {
		if (XaDataSourceModifier.xaResourceObserverFactory == null) {
			return xaDataSource;
		}
		return XaDataSourceModifier.xaResourceObserverFactory.augmentXADataSource(xaDataSource);
	}
}
