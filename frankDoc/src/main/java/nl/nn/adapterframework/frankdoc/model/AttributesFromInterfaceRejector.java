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

package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

/**
 * This class calculates what attributes are excluded because of an ff.ignoreTypeMembership JavaDoc tag.
 * <p>
 * Say that a class C has the ff.ignoreTypeMembership JavaDoc tag and that the argument of the
 * tag is the name of interface I. Now consider all attribute setters that class C
 * defines or inherits. For each corresponding attribute name attr, its excluded or not excluded
 * status is calculated as follows:  
 * <ul>
 * <li> If the attribute setter of attr is not in interface I, then attr is not excluded.
 * <li> Otherwise, if attr is in some interface J that is not an extension of I, then attr is not excluded.
 * <li> Otherwise, attr is excluded. It is excluded because it is in interface I and because that exclusion
 * is not overruled.
 * </ul>
 * <p>
 * How about the attributes that a class D inherits from C? Remember that a FrankElement only holds its
 * declared attributes, not its inherited attributes. By default, D does not hold attributes that are
 * already available because of inheritance from C. For this reason, class D does not have to consider
 * the ff.ignoreTypeMembership tag on C. If D has an attribute setter of an attribute excluded for C,
 * then it is reintroduced for class D.
 * <p>
 * What if interface I has an attribute attr for which class C does not have an attribute setter? If a super class
 * P of C has an attribute setter for attr, how do we prevent attr to be inherited by C? The FrankElement of
 * C should get a FrankAttribute object for attr that is marked as excluded. The AncestorChildNavigation class
 * will then take care of excluding attr from inheritance. The output of {@link #getRejects(FrankClass)}
 * includes attribute names for which the attribute setter is only inherited by C, not necessarily declared.
 * This ensures that all necessary FrankAttribute objects are created.
 * <p>
 * The algorithm works recursively. Assume that C has superclasses and interfaces S1, ..., Sn.
 * Assume that we know for each Sj, j between 1 and n, what attributes have to be excluded
 * and which ones have to be retained (not excluded). Then we can calculated for class C
 * which attributes are excluded and which are retained. The recursion ends because all
 * attributes of I are excluded. Any other class or interface that has no superclass and no
 * super-interfaces has all its attributes retained.
 * 
 * @author martijn
 *
 */
public class AttributesFromInterfaceRejector {
	private final String rejectedInterface;

	private static class Inheriteds {
		Set<String> rejects;
		Set<String> retains;
	}

	/**
	 * @param rejectedInterface The argument of an ff.ignoreTypeMembership annotation.
	 */
	AttributesFromInterfaceRejector(String rejectedInterface) {
		this.rejectedInterface = rejectedInterface;
	}

	Set<String> getRetains(FrankClass clazz) {
		if(rejectedInterface.equals(clazz.getName())) {
			return new HashSet<>();
		} else {
			Inheriteds inheriteds = getInherits(clazz);
			inheriteds.retains.addAll(getAttributeNamesOf(clazz));
			inheriteds.retains.removeAll(inheriteds.rejects);
			return inheriteds.retains;
		}
	}

	/**
	 * @param clazz The owner of the attributes for which we want to know whether they are excluded.
	 * @return The names of the excluded attributes.
	 */
	public Set<String> getRejects(FrankClass clazz) {
		if(rejectedInterface.equals(clazz.getName())) {
			return getAttributeNamesOf(clazz);
		} else {
			Inheriteds inheriteds = getInherits(clazz);
			inheriteds.rejects.removeAll(inheriteds.retains);
			return inheriteds.rejects;
		}
	}

	private List<FrankClass> getSuperClassAndInheritedInterfaces(FrankClass clazz) {
		List<FrankClass> result = new ArrayList<>(Arrays.asList(clazz.getInterfaces()));
		if(clazz.getSuperclass() != null) {
			result.add(clazz.getSuperclass());
		}
		return result;
	}

	private Inheriteds getInherits(FrankClass clazz) {
		Inheriteds result = new Inheriteds();
		result.rejects = new HashSet<>();
		result.retains = new HashSet<>();
		for(FrankClass superClazz: getSuperClassAndInheritedInterfaces(clazz)) {
			result.rejects.addAll(getRejects(superClazz));
			result.retains.addAll(getRetains(superClazz));
		}
		return result;
	}

	Set<String> getAttributeNamesOf(FrankClass clazz) {
		Map<String, FrankMethod> attributesByName = FrankDocModel.getAttributeToMethodMap(clazz.getDeclaredMethods(), "set");
		return new HashSet<>(attributesByName.keySet());
	}
}
