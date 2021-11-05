# Frank!Doc user manual for contributors

The Frank!Doc provides reference information for Frank developers as explained in [CONTRIBUTING.md](./CONTRIBUTING.md). It is produced by a doclet that assembles the Frank!Doc from the Java sources. You can control the Frank!Doc using custom JavaDoc tags and using Frank!Doc-related Java annotations. These annotations and tags are summarized in the table below:


| Kind | Appears on | Name | Function |
| ---- | ---------- | ---- | -------- |
| Java annotation | Class or interface | @FrankDocGroup | Define group as shown in top-left of webapp, fields `name` and `order`. |
| JavaDoc tag | Class | `@ff.parameters` | Describes how parameters (Frank config `<Param>`) are used. |
| JavaDoc tag | Class | `@ff.parameter` | Describes the meaning of a specific parameter. |
| JavaDoc tag | Class | `@ff.forward` | Describes a forward (e.g. `success`, `failure`). |
| JavaDoc tag | Class | `@ff.ignoreTypeMembership` | Suppress inheritance of attributes and webapp group membership. |
| JavaDoc tag | Attribute setter | `@ff.default` | Describes default value. |
| JavaDoc tag | Attribute setter | `@ff.noAttribute` | Suppresses declaration and inheritance of attribute. |
| JavaDoc tag | Attribute setter | `@ff.mandatory` | Makes attribute mandatory in Frank config. |
| JavaDoc tag | Child setter | `@ff.defaultElement` | Set default value of `className` attribute in XSD syntax 1 element. |
| Java annotation | Enum constant | `@EnumLabel` | Set representation required in Frank configs. |

Here is more detailed information about some of these tags and annotations:

**Annotation @FrankDocGroup:**: Has fields `name` and `order` (integer), which is used by the webapp to sort the groups. You can define a group by setting multiple `@FrankDocGroup` annotations with the same `name`. Only one `@FrankDocGroup` annotation of a group should have its `order` field set to avoid ambiguity.

**JavaDoc tag @ff.parameter:** First argument is name of parameter, second argument is description of that parameter.

**JavaDoc tag @ff.forward:** First argument is name of forward, second argument is description.

**JavaDoc tag @ff.ignoreTypeMembership:** Argument is full name of Java interface. The class owning the `@ff.ignoreTypeMembership` does not define or inherit attributes from setters that are overridden from the referenced interface. An attribute is not suppressed if its setter is as well overridden from an ancestor (class or interface) that does not have the referenced interface as ancestor itself. Attributes can be re-introduced in derived classes of the owning class. If the referenced interface has a `@FrankDocGroup` annotation, then the owning class and its descendants are excluded from that group.

**JavaDoc tag @ff.noAttribute:** Attributes can be re-introduced in derived classes by overriding the setter.

