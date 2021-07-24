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
/**
 * This package was created to allow the Frank!Doc model to be instantiated both from Java reflection or from within a doclet.
 * The doclet that creates the Frank!Doc is in package {@link nl.nn.adapterframework.frankdoc.front}. In June 2021,
 * the transition from using reflection to using doclets is complete. We do not need to created the Frank!Doc model
 * from Java reflection anymore. The reflection implementation of this API is therefore removed. 
 * <p>
 * To find classes, instantiate a {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository} using
 * the static method
 * {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository#getDocletInstance(com.sun.javadoc.ClassDoc[], java.util.Set, java.util.Set, java.util.Set)}.
 * This produces a
 * {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepositoryDoclet}, which implements interface
 * {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository}. When you access a
 * {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepositoryDoclet}, you browse
 * classes from the doclet API like {@link com.sun.javadoc.ClassDoc} and {@link com.sun.javadoc.MethodDoc}.
 * Classes that use {@link nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository} do not
 * see whether reflection (no longer present) or doclet objects are being browsed.
 * <p>
 * The arguments of the factory methods are needed to filter the classes to consider. 
 * <p>
 * The picture below shows what classes are available to model elements that appear in Java code.
 * <p>
 * <img src="doc-files/classHierarchy.jpg" width="400" alt="Image classHierarchy.jpg can not be shown" />
 * <p>
 */
package nl.nn.adapterframework.frankdoc.doclet;