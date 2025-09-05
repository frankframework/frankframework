package org.frankframework.jta.xa;

import javax.sql.XADataSource;

import jakarta.annotation.Nonnull;

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

	/**
	 * Remove the current factory, but only if it is of the requested type. Otherwise, throw an IllegalArgumentException
	 */
	public static <T> T removeFactory(@Nonnull Class<T> type) {
		if (type.isInstance(XaDataSourceModifier.xaResourceObserverFactory)) {
			//noinspection unchecked
			T instance = (T)XaDataSourceModifier.xaResourceObserverFactory;
			XaDataSourceModifier.xaResourceObserverFactory = null;
			return instance;
		} else if (XaDataSourceModifier.xaResourceObserverFactory != null) {
			throw new IllegalArgumentException("Cannot remove factory, it is of type [" + XaDataSourceModifier.xaResourceObserverFactory.getClass().getName() + "] instead of requested type [" + type.getName() + "]");
		}
		return null;
	}

	public static XADataSource augmentXADataSource(XADataSource xaDataSource) {
		if (XaDataSourceModifier.xaResourceObserverFactory == null) {
			return xaDataSource;
		}
		return XaDataSourceModifier.xaResourceObserverFactory.augmentXADataSource(xaDataSource);
	}
}
