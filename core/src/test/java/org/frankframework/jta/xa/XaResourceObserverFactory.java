package org.frankframework.jta.xa;

import javax.sql.XADataSource;

public interface XaResourceObserverFactory {
	XADataSource augmentXADataSource(XADataSource xaDataSource);
	void destroy();
}
