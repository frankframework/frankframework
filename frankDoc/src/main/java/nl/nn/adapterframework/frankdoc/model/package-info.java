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
 * This package contains a set of model classes that is used by {@link nl.nn.adapterframework.frankdoc.DocWriterNew} to generate the
 * XML configuration schema used by Frank developers. Please note that {@link nl.nn.adapterframework.frankdoc.DocWriterNew} is
 * presently not used; this class is under development.
 * 
 * <h1>The model</h1>
 *
 * The following diagram introduces the Java classes of the model:
 * <p>
 * <img src="doc-files/FrankDocModel.jpg" width="600" alt="Image FrankDocModel.jpg can not be shown" />
 * <p>
 * Class {@link nl.nn.adapterframework.frankdoc.model.FrankElement} models a Java class of the Frank!Framework that can be accessed from a
 * Frank config. An example is {@link nl.nn.adapterframework.frankdoc.model.FrankElement} <code>nl.nn.adapterframework.parameters.Parameter</code>,
 * which you can reference in a Frank config with the tag <code>&lt;Param&gt;</code>. The modeled Java class
 * can have a superclass, which is expressed by the link named "parent".
 * <p>
 * A tag in a Frank config can contain other tags. A <code>&lt;Receiver&gt;</code> can
 * for example contain <code>&lt;DirectQuerySender&gt;</code> or
 * <code>DirectQueryErrorSender</code>. These two tags reference the same Java class, namely
 * {@code nl.nn.adapterframework.jdbc.DirectQuerySender}, but the first tag uses
 * it as a Sender while the second tag uses it as an ErrorSender. The model expresses a set
 * of allowed child tags by relating a containing {@link nl.nn.adapterframework.frankdoc.model.FrankElement} to an {@link nl.nn.adapterframework.frankdoc.model.ElementRole}.
 * An {@link nl.nn.adapterframework.frankdoc.model.ElementRole} has a property <code>roleName</code> to express the role and references
 * an {@link nl.nn.adapterframework.frankdoc.model.ElementType} to define what child {@link nl.nn.adapterframework.frankdoc.model.FrankElement} objects can appear. Each
 * {@link nl.nn.adapterframework.frankdoc.model.ElementType} has one or more {@link nl.nn.adapterframework.frankdoc.model.FrankElement} objects as members.
 * <p>
 * There are two
 * flavors of {@link nl.nn.adapterframework.frankdoc.model.ElementType} objects. Some {@link nl.nn.adapterframework.frankdoc.model.ElementType} objects model a Java
 * interface. In this case, the members are the {@link nl.nn.adapterframework.frankdoc.model.FrankElement} objects that model
 * the Java classes that implement the Java interface. Please note that not every Java interface
 * that appears in the Java source code is modeled by an {@link nl.nn.adapterframework.frankdoc.model.ElementType} object. An
 * {@link nl.nn.adapterframework.frankdoc.model.ElementType} object appears only for Java interfaces that are relevant for nesting
 * tags in a Frank config. Some {@link nl.nn.adapterframework.frankdoc.model.ElementType} objects model Java interfaces that have
 * an inheritance relation. This inheritance is expressed in the model using the "highest common interface"
 * relation. This relation is needed for some corner cases of generating the XML schema file.
 * <p>
 * Other {@link nl.nn.adapterframework.frankdoc.model.ElementType} objects
 * just model a single Java class, which is expressed by a reference to the corresponding
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement} as the only member.
 * <p>
 * There is no direct relation between {@link nl.nn.adapterframework.frankdoc.model.FrankElement} an {@link nl.nn.adapterframework.frankdoc.model.ElementRole}, because the
 * relation between a parent tag and a child tag requires some additional information.
 * {@link nl.nn.adapterframework.frankdoc.DocWriterNew} requires information on how often a child tag can appear, whether
 * usage of the child tag is deprecated, and some other information. This additional information
 * is included in class {@link nl.nn.adapterframework.frankdoc.model.ConfigChild}, which also references the {@link nl.nn.adapterframework.frankdoc.model.FrankElement} of
 * the parent tag and the {@link nl.nn.adapterframework.frankdoc.model.ElementRole} for a set of allowed child tags.
 * <p>
 * Tags in Frank configs can have attributes, which are modeled by class {@link nl.nn.adapterframework.frankdoc.model.FrankAttribute}.
 * Attributes have a type that is modeled by {@link nl.nn.adapterframework.frankdoc.model.AttributeType} (not in diagram).
 * String attributes can have their values restricted by a Java enum type. In that case, the list of allowed values is stored in a
 * {@link nl.nn.adapterframework.frankdoc.model.AttributeEnum}, which can be shared by multiple {@link nl.nn.adapterframework.frankdoc.model.FrankAttribute}. 
 * The tag in which the attribute occurs is modeled by its {@link nl.nn.adapterframework.frankdoc.model.FrankElement}, see relation
 * "attribute of". The documentation of an attribute may appear in a Java class that differs
 * from the attribute owning Java class (the IbisDocRef Java annotation). This is expressed
 * by the relation "described by".
 * <p>
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement} only hold the attributes and config children that are
 * declared by the modeled Java class. Attributes or config children owned by inheritance are not modeled
 * directly as children. {@link nl.nn.adapterframework.frankdoc.model.FrankElement} has methods to browse
 * inherited children, which need a callback object of type {@link nl.nn.adapterframework.frankdoc.model.CumulativeChildHandler}.
 * This functionality works the same for attributes and config children. Therefore,
 * {@link nl.nn.adapterframework.frankdoc.model.FrankAttribute} and {@link nl.nn.adapterframework.frankdoc.model.ConfigChild}
 * have a common superclass {@link nl.nn.adapterframework.frankdoc.model.ElementChild}. That class also
 * parses IbisDoc annotations, which is done the same way for config children and attributes.
 * <p>
 * The model is not only used to generate the XML Schema file (<code>ibisdoc.xsd</code>). It is also
 * used to generate a website with reference documentation. That website documents all tags
 * ({@link nl.nn.adapterframework.frankdoc.model.FrankElement} objects) that can be used. The {@link nl.nn.adapterframework.frankdoc.model.FrankElement} objects are grouped
 * by the implemented Java interface (interface-implementing {@link nl.nn.adapterframework.frankdoc.model.ElementType} objects). There
 * is an additional group "Other" for all {@link nl.nn.adapterframework.frankdoc.model.FrankElement} that belong to a non-interface-based
 * {@link nl.nn.adapterframework.frankdoc.model.ElementType} (e.g. {@link nl.nn.adapterframework.core.PipeForward}). These table-of-contents (TOC)
 * groups are modeled by model class {@link nl.nn.adapterframework.frankdoc.model.FrankDocGroup}.
 * <p>
 * Class {@link nl.nn.adapterframework.frankdoc.model.FrankDocModel} holds the entire model (not shown in the diagram). Two model classes
 * have not been explained yet: {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet} and
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}. These have been introduced to avoid conflicts
 * in the XML schema file. These conflicts are explained in the next section.
 * 
 * <h1>Conflicts</h1>
 *
 * <h2>Element names from FrankElement</h2>
 *
 * A {@link nl.nn.adapterframework.frankdoc.model.FrankElement} models a Java class that Frank
 * developers reference using an XML tag. Method {@link nl.nn.adapterframework.frankdoc.model.FrankElement#getXsdElementName(ElementRole)}
 * calculates the name of this XML element. Remember that XML elements in Frank configs not only
 * express the referenced Java class but also the role in which this Java class is used. This is
 * why the method has an {@link nl.nn.adapterframework.frankdoc.model.ElementRole} argument.
 * <p>
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s in
 * {@link nl.nn.adapterframework.frankdoc.model.FrankDocGroup} "Other",
 * like <code>&lt;Forward&gt;</code> and <code>&lt;Param&gt;</code>, are
 * named in a special way. They usually belong to a single
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole}. Their name is
 * based on the syntax 1 name of that {@link nl.nn.adapterframework.frankdoc.model.ElementRole}.
 * <p>
 * This way of naming introduces potential conflicts when deprecated
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s are included. These potential conflicts
 * are explained in the following subsections, with the solutions applied to prevent them. There are
 * also some other potential conflicts. These are explained as well.
 * 
 * <h2>ElementRole member conflicts</h2>
 * 
 * A Pipeline can have pipes as children, which appears in the Java code as a config child setter:
 * {@link nl.nn.adapterframework.core.PipeLine#addPipe(nl.nn.adapterframework.core.IPipe)}.
 * The resulting config child has an {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * that we can express as (IPipe, pipe), IPipe representing the
 * {@link nl.nn.adapterframework.frankdoc.model.ElementType} and "pipe" is the syntax 1 name.
 * The members of this element role include the following: 
 * {@link nl.nn.adapterframework.pipes.FixedResult} and {@link nl.nn.adapterframework.pipes.FixedResultPipe}.
 * Method {@link nl.nn.adapterframework.frankdoc.model.FrankElement#getXsdElementName(ElementRole)} gives
 * them the same name, which is <code>FixedResultPipe</code>.
 * <p>
 * To avoid this type of conflicts, element role member conflicts, we do the following for each
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole}.
 * We collect all members ({@link nl.nn.adapterframework.frankdoc.model.FrankElement}) and group them by
 * the XML element name that these members get for the role. When an XML element name
 * is shared by multiple {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s, we assume
 * that the conflict has arisen because the core team intended to rename the Java class behind
 * the element name. The old Java class is needed for backwards compatibility,
 * but it has been deprecated to support resolving the conflict. This assumption allows us
 * to omit the deprecated {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s. We
 * emit a warning in case the conflict remains unresolved.
 * 
 * <h2>The generic element option</h2>
 *
 * The XML schema files we produce are needed by the Frank!Framework to parse
 * configurations. To make this possible, our XML schemas need to support syntax 1, which
 * means syntax like <code>&lt;Listener className="nl.nn...."&gt;</code>.
 * This element is allowed as a child of each XML element that is allowed already
 * to contain a particular listener as child, for example as a child of <code>&lt;Receiver&gt;</code>.
 * We say that <code>&lt;Listener&gt;</code> is the generic element option of
 * <code>&lt;Receiver&gt;</code>.
 * <p>
 * Our XML schemas should specify which 
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s are allowed as
 * children of a generic element option. These {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s
 * do not have to be written out explicitly, because we can reference
 * XML schema groups that are based on {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s,
 * the member children. The member children of an {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * are found by collecting all config children of all members, and taking
 * the {@link nl.nn.adapterframework.frankdoc.model.ElementRole} of each
 * {@link nl.nn.adapterframework.frankdoc.model.ConfigChild}. The generic element
 * option is defined by adding a group reference for each relevant member child, the
 * set of relevant member children depending on the XML schema version being created.
 * <p>
 * In some cases, multiple {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s have to
 * share a generic element option. The example is
 * {@link nl.nn.adapterframework.batch.StreamTransformerPipe}. It has
 * config children with the followin {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s:
 *
 * <ul>
 * <li>(IRecordHandlerManager, child)
 * <li>(RecordHandlingFlow, child)
 * <li>(IRecordHandler, child)
 * <li>(IResultHandler, child)
 * </ul>
 * 
 * If all of these would be used to define the generic element option <code>&lt;Child&gt;</code>,
 * the XML schema would be invalid. If the parser would encounter <code>&lt;Child&gt;</code>, it
 * would not know which definition to apply.
 * <p>
 * This potential conflict is resolved by merging config children. When a
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement} has cumulative
 * config children sharing a syntax 1 name, then these config children
 * are merged. This check is done by method {@link nl.nn.adapterframework.frankdoc.model.FrankElement#hasOrInheritsPluralConfigChildren(java.util.function.Predicate, java.util.function.Predicate)},
 * which takes {@link java.util.function.Predicate} arguments that express
 * the XML schema version being created. Config children sharing a common
 * syntax 1 name for the XML schema version being created are called "plural config children".
 * <p>
 * To be able to handle plural config children, we introduce model entity {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}.
 * All cumulative config children of a {@link nl.nn.adapterframework.frankdoc.model.FrankElement} are grouped
 * by syntax 1 name, and each group results in a {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}.
 * We use {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}-s instead of {@link nl.nn.adapterframework.frankdoc.model.ConfigChild}-s
 * to fill XSD element groups, resulting in common code for plural and non-plural config children.
 * 
 * <h2>Conflicts by ElementType interface inheritance</h2>
 *
 * Listeners are Frank elements that receive incoming messages. A Java class within
 * the Frank!Framework is a listener when it implements {@link nl.nn.adapterframework.core.IListener}.
 * There is a potential conflict because there are Java interfaces that inherit from
 * {@link nl.nn.adapterframework.core.IListener}. There is a pipe
 * {@link nl.nn.adapterframework.pipes.PostboxRetrieverPipe} with
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole} (IPostboxListener, listener).
 * We also have {@link nl.nn.adapterframework.pipes.SenderPipe} with
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole} (ICorrelatedPullingListener, listener).
 * These two pipes are members of role (IPipe, pipe). There is a generic element
 * option <code>&lt;Pipe&gt;</code> that can have an element <code>&lt;Listener&gt;</code>.
 * But if both listener {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s
 * (IPostboxListener, listener) and (ICorrelatedPullingListener, listener) would
 * be used to define the <code>&lt;Listener&gt;</code>, a conflict would result.
 * If the parser would encounter <code>&lt;Listener&gt;</code>, it would not know
 * which of the two definitions to use.
 * <p>
 * This conflict is resolved by promoting the two {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s
 * to (IListener, listener). Every {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * is used only once and thus this promotion resolves the conflict. We call it the
 * "highest common interface", which is calculated by method
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole#getHighestCommonInterface()}.
 *
 * <h2>Member conflicts with element name of generic element option</h2>
 * 
 * We have an {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * (IErrorMessageFormatter, errorMessageFormatter) which has
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement}
 * {@link nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter}
 * as member. Element groups including a generic element option
 * <code>&lt;ErrorMessageFormatter&gt;</code> cannot also define
 * XML element <code>&lt;ErrorMessageFormatter&gt;</code> to reference
 * Java class {@link nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter}.
 * This conflict with the element name of the generic element option is found
 * by method {@link nl.nn.adapterframework.frankdoc.model.ElementRole#getDefaultElementOptionConflict()}
 * by that XML element. 
 * 
 * <h2>Member conflicts in shared generic element option</h2>
 *
 * Members of a generic element options shared by plural config children can introduce a conflict.
 * An example occurs in {@link nl.nn.adapterframework.frankdoc.model.FrankElement}
 * {@link nl.nn.adapterframework.batch.AbstractRecordHandler}. It has the following
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s:
 * 
 * <ul>
 * <li> (InputfieldsPart, child)
 * <li> (OutputfieldsPart, child)
 * </ul>
 *
 * These are non-interface-based {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s, so these
 * would both define XML element <code>&lt;Child&gt;</code>. This is not possible. Syntax 2
 * does not provide the option to reference Java class <code>InputfieldsPart</code>
 * or <code>OutputfieldsPart</code> in role "child". This is not a big issue, because
 * they are available in roles "inputFields" and "outputFields". Furthermore, they can
 * still be expressed in syntax 1: <code>&lt;Child className="...InputfieldsPart"&gt;</code>.
 * <p>
 * The config child setters for (InputfieldsPart, child) and (OutputfieldsPart, child)
 * are deprecated. One could wonder why we do not try to resolve the conflict by
 * omitting deprecated config children. In theory, a FF! version could have
 * existed in which the config child setter for (InputfieldsPart, child) was not deprecated.
 * Then that version could define <code>&lt;Child&gt;</code> to reference
 * Java class <code>InputfieldsPart</code> in role "child". We do not want this, however,
 * because it threats backward compatibility. We would have old Frank configs including
 * <code>&lt;Child&gt;</code>, but they would be incorrect now because of the conflict.
 * <p>
 * To detect member conflicts in shared generic element options, the model has entity
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}. Each
 * {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet} is linked to an {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}
 * that has the element roles of the config children of the {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}
 * as members. When multiple {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}-s reference the
 * same set of {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s, then they reference
 * a common {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet} object. Each
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole} knows the {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}-s
 * it belongs to. {@link nl.nn.adapterframework.frankdoc.model.ElementRole} uses this information to
 * omit members that would conflict with a shared generic element option. Class
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet} is not public, because
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole} takes care of calling
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}. 
 * 
 * <h2>Generic element option recursion</h2>
 *
 * This subsection describes a conflict that does not occur in version 7.6-SNAPSHOT
 * of the Frank!Framework. It is covered by the test input classes in package
 * <code>nl.nn.adapterframework.frankdoc.testtarget.exotic</code>. There is a class
 * <code>Master</code> with {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * (IMember, part). This role has no {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-members
 * with plural config children, but there is still a conflict. All child members of this role should
 * be used to find child XML elements of <code>&lt;Master&gt;&ltPart&gt;...</code>. We have
 * {@link nl.nn.adapterframework.frankdoc.model.FrankElement}-s <code>Member1</code> and
 * <code>Member2</code> that have config children with the following {@link nl.nn.adapterframework.frankdoc.model.ElementRole}-s:
 * (Child1, child) and (Child2, child). These are not interface-based, but we cannot
 * define element <code>&lt;Master&gt;&ltPart&gt;&lt;Child&gt;</code> to reference
 * them both. We would have two conflicting definitions, because it is not clear whether this
 * XML element would reference to <code>Child1</code> or <code>Child2</code>.
 * <p>
 * We solve this issue by omitting syntax 2 elements for Java classes <code>Child1</code> and <code>Child2</code>.
 * These classes are only available through syntax 1: <code>&lt;Child className="..."&gt;</code>.
 * This is implemented in two places. First, we introduce more {@link nl.nn.adapterframework.frankdoc.model.ElementRole}
 * objects. For each {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}-based
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}, we introduce new
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}-s by calculating the member
 * children and grouping them by syntax 1 name. We do this recursively. The recursion
 * will stop because the recursion will find sets of element roles that correspond
 * to existing {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet} objects.
 * <p>
 * Omitting syntax 2 elements for these conflicts is also done in {@link nl.nn.adapterframework.frankdoc.DocWriterNew}.
 * The example shows that the contents of a generic element option does not
 * necessarily correspond to a {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet}.
 * To fill a generic element option, we need to get a {@link java.util.Set} of
 * {@link nl.nn.adapterframework.frankdoc.model.ElementRole} holding the roles that are
 * selected for the XML version being created. Then we have to calculate member children
 * recursively from these element role sets. The model provides static method
 * {@link nl.nn.adapterframework.frankdoc.model.ConfigChildSet#getMemberChildren(java.util.List, java.util.function.Predicate, java.util.function.Predicate, java.util.function.Predicate)}
 * to support this. We cannot use {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}
 * for this in {@link nl.nn.adapterframework.frankdoc.DocWriterNew}, because {@link nl.nn.adapterframework.frankdoc.DocWriterNew}
 * has to filter roles for the chosen version of the XML schema. Class {@link nl.nn.adapterframework.frankdoc.model.ElementRoleSet}
 * works without filtering role. That class has to find conflicting {@link nl.nn.adapterframework.frankdoc.model.FrankElement},
 * the conflicts not being affected by the version of the XML schema being created.
 *
 */
package nl.nn.adapterframework.frankdoc.model;

