/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.ejb;

/**
 * Local Home interface for Enterprise Bean: IbisManagerEjb
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public interface IbisManagerEjbLocalHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: IbisManagerEjb
     */
    public IbisManagerEjbLocal create()
        throws javax.ejb.CreateException;
}
