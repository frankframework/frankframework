/*
   Copyright 2022 - 2024 WeAreFrank!

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
package org.frankframework.http.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.context.event.ContextRefreshedEvent;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FrankCXFServlet extends CXFServlet {

	@Override
	public void setBus(Bus bus) {
		if(bus != null) {
			String busInfo = "Successfully created %s with SpringBus [%s]".formatted(getServletName(), bus.getId());
			log.info(busInfo);
			getServletContext().log(busInfo);
		}

		super.setBus(bus);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}
}
