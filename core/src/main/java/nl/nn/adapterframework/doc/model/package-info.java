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
 * This package contains a set of model classes that is used by {@link nl.nn.adapterframework.doc.DocWriterNew} to generate ibisdoc.xsd,
 * the XML configuration schema used by Frank developers.
 * <p>
 * Please note that {@link nl.nn.adapterframework.doc.DocWriterNew} is presently not used; this class is under development. The following
 * diagram introduces the Java classes of the model:
 * <p>
 * <img src="doc-files/FrankDocModel.jpg" width="600" alt="Image FrankDocModel.jpg can not be shown" />
 * <p>
 * Class {@link nl.nn.adapterframework.doc.model.FrankElement} models a Java class of the Frank!Framework that can be accessed from a
 * Frank config. An example is {@link nl.nn.adapterframework.doc.model.FrankElement} <code>nl.nn.adapterframework.parameters.Parameter</code>,
 * which you can reference in a Frank config with the tag <code>&lt;Param&gt;</code>. The modeled Java class
 * can have a superclass, which is expressed by the link named "parent".
 * <p>
 * A tag in a Frank config can contain other tags. A <code>&lt;Receiver&gt;</code> can
 * for example contain <code>&lt;DirectQuerySender&gt;</code> or
 * <code>&lt;DirectQueryErrorSender&gt;</code>. These two tags reference the same Java class, namely
 * {@code nl.nn.adapterframework.jdbc.DirectQuerySender}, but the first tag uses
 * it as a Sender while the second tag uses it as an ErrorSender. The model expresses the
 * allowed child tags by relating the containing {@link nl.nn.adapterframework.doc.model.FrankElement} to some {@link nl.nn.adapterframework.doc.model.ElementRole}.
 * Each combination of a {@link nl.nn.adapterframework.doc.model.FrankElement} and a {@link nl.nn.adapterframework.doc.model.ElementRole}
 * corresponds to a config child setter (Java method) in the modelled Java class. When a Java class has multiple config child setters,
 * its {@link nl.nn.adapterframework.doc.model.FrankElement} is related to multiple {@link nl.nn.adapterframework.doc.model.ElementRole}.
 * An {@link nl.nn.adapterframework.doc.model.ElementRole} has a property <code>syntax1Name</code> to express the role and references
 * an {@link nl.nn.adapterframework.doc.model.ElementType} to define what child {@link nl.nn.adapterframework.doc.model.FrankElement} objects can appear. Each
 * {@link nl.nn.adapterframework.doc.model.ElementType} has one or more {@link nl.nn.adapterframework.doc.model.FrankElement} objects as members.
 * <p>
 * There are two
 * flavors of {@link nl.nn.adapterframework.doc.model.ElementType} objects. Some {@link nl.nn.adapterframework.doc.model.ElementType} objects model a Java
 * interface. In this case, the members are the {@link nl.nn.adapterframework.doc.model.FrankElement} objects that model
 * the Java classes that implement the Java interface. Please note that not every Java interface
 * that appears in the Java source code is modeled by an {@link nl.nn.adapterframework.doc.model.ElementType} object. An
 * {@link nl.nn.adapterframework.doc.model.ElementType} object appears only for Java interfaces that are relevant for nesting
 * tags in a Frank config. Some {@link nl.nn.adapterframework.doc.model.ElementType} objects model Java interfaces that have
 * an inheritance relation. This inheritance is expressed in the model using the "highest common interface"
 * relation. This relation is needed for some corner cases of generating the XML schema file.
 * <p>
 * Other {@link nl.nn.adapterframework.doc.model.ElementType} objects
 * just model a single Java class, which is expressed by a reference to the corresponding
 * {@link nl.nn.adapterframework.doc.model.FrankElement} as the only member.
 * <p>
 * There is no direct relation between {@link nl.nn.adapterframework.doc.model.FrankElement} an {@link nl.nn.adapterframework.doc.model.ElementRole}, because the
 * relation between a parent tag and a child tag requires some additional information.
 * {@link nl.nn.adapterframework.doc.DocWriterNew} requires information on how often a child tag can appear, whether
 * usage of the child tag is deprecated, and some other information. This additional information
 * is included in class {@link nl.nn.adapterframework.doc.model.ConfigChild}, which also references the {@link nl.nn.adapterframework.doc.model.FrankElement} of
 * the parent tag and the {@link nl.nn.adapterframework.doc.model.ElementRole} for a set of allowed child tags.
 * <p>
 * Tags in Frank configs can have attributes, which are modeled by class {@link nl.nn.adapterframework.doc.model.FrankAttribute}.
 * The tag in which the attribute occurs is modeled by its {@link nl.nn.adapterframework.doc.model.FrankElement}, see relation
 * "attribute of". The documentation of an attribute may appear in a Java class that differs
 * from the attribute owning Java class (the IbisDocRef Java annotation). This is expressed
 * by the relation "described by".
 * <p>
 * {@link nl.nn.adapterframework.doc.model.FrankElement} only hold the attributes and config children that are
 * declared by the modeled Java class. Attributes or config children owned by inheritence are not modeled
 * directly as children. {@link nl.nn.adapterframework.doc.model.FrankElement} has methods to browse
 * inherited children, which need a callback object of type {@link nl.nn.adapterframework.doc.model.CumulativeChildHandler}.
 * This functionality works the same for attributes and config children. Therefore,
 * {@link nl.nn.adapterframework.doc.model.FrankAttribute} and {@link nl.nn.adapterframework.doc.model.ConfigChild}
 * have a common superclass {@link nl.nn.adapterframework.doc.model.ElementChild}.
 * <p>
 * For each config child, {@link nl.nn.adapterframework.doc.DocWriterNew} builds an XML Schema group that lists
 * all the XML Schema elements that can appear as children. Such a group should not only support syntax 2 elements,
 * but it should also add a generic element option that allows the Frank developer to use syntax 1. As an example,
 * a &lt;Receiver&gt; can contain an <code>&lt;ApiListener&gt;</code>, but it can also hold
 * <code>&lt;Listener className="nl.nn.adapterframework.http.rest.ApiListener" ...&gt;</code>.
 * <p>
 * There are {@link nl.nn.adapterframework.doc.model.FrankElement} for which multiple config
 * children share the same syntax 1 name. The example is {@link nl.nn.adapterframework.batch.StreamTransformerPipe}.
 * If every config child would add its own generic element option (e.g. <code>&lt;Child className = ...&gt;</code>),
 * the XML schema would not work. If a Frank developer would put <code>&lt;StreamTransformerPipe&gt;&lt;Child ... &gt;</code>,
 * then the schema validation cannot figure out which part of the XML Schema file applies to thie child.
 * <p>
 * When multiple config children share a syntax 1 name, then a common generic element option is needed for the
 * involved config children. This implies that the sequence of the config children can not be specified in the
 * XML Schema file. It also implies that the contents of a config child cannot always be based on a single
 * {@link nl.nn.adapterframework.doc.model.ElementRole}. We introduce element {@link nl.nn.adapterframework.doc.model.GenericRole},
 * which holds the combination of a syntax 1 name and a list of {@link nl.nn.adapterframework.doc.model.ElementRole}
 * sharing that syntax 1 name.
 * {@link nl.nn.adapterframework.doc.DocWriterNew} will use {@link nl.nn.adapterframework.doc.model.GenericRole} objects
 * to define unordered groups of allowed XML elements. {@link nl.nn.adapterframework.doc.model.GenericRole} can always
 * be used instead of {@link nl.nn.adapterframework.doc.model.ElementRole}, because we also create
 * {@link nl.nn.adapterframework.doc.model.GenericRole} objects that hold only one
 * {@link nl.nn.adapterframework.doc.model.ElementRole}.
 * <p>
 * The model is not only used to generate the XML Schema file (<code>ibisdoc.xsd</code>). It is also
 * used to generate a website with reference documentation. That website documents all tags
 * ({@link nl.nn.adapterframework.doc.model.FrankElement} objects) that can be used. The {@link nl.nn.adapterframework.doc.model.FrankElement} objects are grouped
 * by the implemented Java interface (interface-implementing {@link nl.nn.adapterframework.doc.model.ElementType} objects). There
 * is an additional group "Other" for all {@link nl.nn.adapterframework.doc.model.FrankElement} that belong to a non-interface-based
 * {@link nl.nn.adapterframework.doc.model.ElementType} (e.g. {@link nl.nn.adapterframework.core.PipeForward}). These table-of-contents (TOC)
 * groups are modeled by model class {@link nl.nn.adapterframework.doc.model.FrankDocGroup}.
 * <p>
 * Finally, class {@link nl.nn.adapterframework.doc.model.FrankDocModel} holds the entire model (not shown in the diagram).
 * It saves which {@link nl.nn.adapterframework.doc.model.ElementType},
 * {@link nl.nn.adapterframework.doc.model.ElementRole} and
 * {@link nl.nn.adapterframework.doc.model.FrankElement} exist and it ensures that
 * these objects are created only once. Therefore, these classes can be compared using the
 * standard {@link java.lang.Object#equals(java.lang.Object)} method. We want the same
 * for {@link nl.nn.adapterframework.doc.model.GenericRole}, but creation of these
 * objects is different depending on whether <code>strict.xsd</code> or <code>compatibility.xsd</code>
 * is being developed. We solve this by introducing {@link nl.nn.adapterframework.doc.model.XsdVersion}.
 * {@link nl.nn.adapterframework.doc.model.FrankDocModel} holds a different collection of
 * {@link nl.nn.adapterframework.doc.model.GenericRole} for each
 * {@link nl.nn.adapterframework.doc.model.XsdVersion}.
 */
package nl.nn.adapterframework.doc.model;

