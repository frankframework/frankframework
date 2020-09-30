/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.jmx.JmxOperation;
import nl.nn.adapterframework.util.RunStateEnum;
/**
 * Models starting and stopping of objects that support such behaviour.
 *
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface IManagable extends INamedObject {
    /**
     * returns the runstate of the object.
     * Possible values are defined by {@link RunStateEnum}.
     */
    RunStateEnum getRunState();

    /**
     * Instruct the object that implements <code>IManagable</code> to start working.
     * The method does not wait for completion of the command; at return of this method,
     * the object might be still in the STARTING-runstate
     */
    @JmxOperation(description = "Start the Adapter")
    void startRunning();

    /**
     * Instruct the object that implements <code>IManagable</code> to stop working.
     * The method does not wait for completion of the command; at return of this method,
     * the object might be still in the STOPPING-runstate
     */
    @JmxOperation(description = "Stop the Adapter")
    void stopRunning();
}
