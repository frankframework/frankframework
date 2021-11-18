# Frank!Doc user manual for contributors

The Frank!Doc provides reference information for Frank developers as explained in [CONTRIBUTING.md](./CONTRIBUTING.md). It is produced by a doclet that assembles the Frank!Doc from the Java sources. You can control the Frank!Doc using custom JavaDoc tags and using Frank!Doc-related Java annotations. These annotations and tags are summarized in the table below:


| Kind | Name | Appears on | Function |
| ---- | ---- | ---------- | -------- |
| Java annotation | `@FrankDocGroup` | Class or interface | Define group as shown in top-left of webapp. |
| JavaDoc tag | `@ff.parameters` | Class | Describes how parameters (Frank config `<Param>`) are used. |
| JavaDoc tag | `@ff.parameter` | Class | Describes the meaning of a specific parameter. |
| JavaDoc tag | `@ff.forward` | Class | Describes a forward (e.g. `success`, `failure`). |
| JavaDoc tag | `@ff.ignoreTypeMembership` | Class | Suppress inheritance of attributes and webapp group membership. |
| JavaDoc tag | `@ff.default` | Attribute setter | Describes default value. |
| JavaDoc tag | `@ff.noAttribute` | Attribute setter | Suppresses declaration and inheritance of attribute. |
| JavaDoc tag | `@ff.mandatory` | Attribute setter | Makes attribute mandatory in Frank config. |
| JavaDoc tag | `@ff.defaultElement` | Child setter | Set default value of `className` attribute in XSD syntax 1 element. |
| Java annotation | `@EnumLabel` | Enum constant | Set representation required in Frank configs. |

Here is more detailed information about some of these tags and annotations:

**Annotation @FrankDocGroup:**: Has fields `name` and `order` (integer), which is used by the webapp to sort the groups. You can define a group by setting multiple `@FrankDocGroup` annotations with the same `name`. Only one `@FrankDocGroup` annotation of a group should have its `order` field set to avoid ambiguity. This annotation behaves differently on classes and interfaces. When a Java class implements an interface that has a `@FrankDocGroup` annotation, then the class is put in the group non-exclusively. A class can belong to multiple groups when it implements multiple interfaces with different groups. However, when a class has a `@FrankDocGroup` annotation, then the class only belongs to that group. `@FrankDocGroup` annotations on classes are inherited by descendant classes, so descendant classes are also in the specified group exclusively.

**JavaDoc tag @ff.parameter:** First argument is name of parameter, second argument is description of that parameter.

**JavaDoc tag @ff.forward:** First argument is name of forward, second argument is description.

**JavaDoc tag @ff.noAttribute:** Attributes can be re-introduced in derived classes by overriding the setter.

