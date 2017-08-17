/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.nn.adapterframework.validation.xerces_2_11;

import org.apache.commons.lang.NotImplementedException;
import org.apache.xerces.xni.grammars.XMLGrammarPool;

/**
 * <p>Implementation of Schema for W3C XML Schemas.</p>
 * 
 * Class copied from Xerces 2.11.0, to support creating ValidatorHandler from GrammarPool in Xerces 2.9.1.
 * 
 * @author Michael Glavassevich, IBM
 * @version $Id: XMLSchema.java 447235 2006-09-18 05:01:44Z mrglavas $
 */
final class XMLSchema extends AbstractXMLSchema {
    
    /** The grammar pool is immutable */
    private final XMLGrammarPool fGrammarPool;
    
    /** Whether to consider this schema to be fully composed */
    private final boolean fFullyComposed;
    
    /** Constructors */
    public XMLSchema(XMLGrammarPool grammarPool) {
        this(grammarPool, true);
    }
    
    public XMLSchema(XMLGrammarPool grammarPool, boolean fullyComposed) {
        fGrammarPool = grammarPool;
        fFullyComposed = fullyComposed;
    }
    
    /*
     * XSGrammarPoolContainer methods
     */
    
    /**
     * <p>Returns the grammar pool contained inside the container.</p>
     * 
     * @return the grammar pool contained inside the container
     */
    public XMLGrammarPool getGrammarPool() {
        return fGrammarPool;
    }
    
    /**
     * <p>Returns whether the schema components contained in this object
     * can be considered to be a fully composed schema and should be
     * used to exclusion of other schema components which may be
     * present elsewhere.</p>
     * 
     * @return whether the schema components contained in this object
     * can be considered to be a fully composed schema
     */
    public boolean isFullyComposed() {
        return fFullyComposed;
    }

    // It appears to be necessary to implement this method, in order to be able to compile via maven.
    public String getProperty(String property) {
    	throw new NotImplementedException("XmlSchema.getProperty()");
    }
} // XMLSchema
