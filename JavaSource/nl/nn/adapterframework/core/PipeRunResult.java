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
/*
 * $Log: PipeRunResult.java,v $
 * Revision 1.6  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2004/03/30 07:29:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The PipeRunResult is a type to store both the result of the processing of a message
 * in {@link IPipe#doPipe(Object, PipeLineSession) doPipe()} as well as the exitState.
 * <br/>
 * <b>Responsibility:</b><br/>
 * <ul><li>keeper of the result of the execution of a <code>Pipe</code></li>
 *     <li>keeper of the forward to be returned to the <code>PipeLine</code></li>
 * </ul><br/>
 * <code>Pipe</code>s return a <code>PipeRunResult</code> with the information
 * as above.
 * 
 * @version $Id$
 * @author Johan Verrips
 * @see PipeForward
 * @see nl.nn.adapterframework.pipes.AbstractPipe#doPipe
 * @see nl.nn.adapterframework.pipes.AbstractPipe#findForward
 */
public class PipeRunResult {

    private PipeForward pipeForward;
    private Object result;
    public PipeRunResult() {
        super();
    }
    public PipeRunResult(PipeForward forward, Object result){
        this.pipeForward=forward;
        this.result=result;
    }
    public PipeForward getPipeForward() {
        return pipeForward;
    }
    public Object getResult() {
        return result;
    }
    public void setPipeForward(PipeForward pipeForward) {
        this.pipeForward = pipeForward;
    }
    public void setResult(Object result) {
        this.result = result;
    }
    /**
     * uses reflection to create the ToString
     */
    public String toString(){
      return ToStringBuilder.reflectionToString(this);
    }
}
