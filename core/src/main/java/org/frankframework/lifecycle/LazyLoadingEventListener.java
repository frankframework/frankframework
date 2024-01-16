/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.lifecycle;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * During the ApplicationContext refresh Spring will try and automatically create and register all EventListeners
 * EventListeners which implement this interface will be exempt from this behaviour but in turn will need to be
 * registered manually in the required org.springframework.context.ConfigurableApplicationContext.
 *
 * @author Niels Meijer
 */
public interface LazyLoadingEventListener<T extends ApplicationEvent> extends ApplicationListener<T> {

}
