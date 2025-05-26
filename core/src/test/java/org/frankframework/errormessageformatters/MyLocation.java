package org.frankframework.errormessageformatters;

import org.frankframework.core.HasName;

public class MyLocation implements HasName {
	@Override
	public String getName() {
		return "dummy-location";
	}
}
