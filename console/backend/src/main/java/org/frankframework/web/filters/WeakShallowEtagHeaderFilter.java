package org.frankframework.web.filters;

import org.springframework.web.filter.ShallowEtagHeaderFilter;

public class WeakShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

	WeakShallowEtagHeaderFilter(){
		super();
		this.setWriteWeakETag(true);
	}

}
