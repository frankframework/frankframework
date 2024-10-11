/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.validation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.core.IScopeProvider;

/**
 * The representation of a XSD.
 */
public interface IXSD extends Schema, Comparable<IXSD> {

	String getResourceTarget();
	String getNamespace();			// the externally configured targetNamespace
	String getTargetNamespace();	// the targetNamespace as defined in the schema
	boolean isAddNamespaceToSchema();
	List<String> getRootTags();
	Set<String> getImportedNamespaces();

	@Nonnull
	String asString() throws IOException;

	@Nullable
	IXSD getImportParent();

	default boolean hasDependency(Set<IXSD> xsds) {
		for (IXSD xsd : xsds) {
			if (getImportedNamespaces().contains(xsd.getTargetNamespace())) {
				return true;
			}
		}
		return false;
	}

	Set<String> getImportedSchemaLocationsToIgnore();
	boolean isUseBaseImportedSchemaLocationsToIgnore();
	Set<String> getImportedNamespacesToIgnore();
	String getParentLocation();

	String getXsdDefaultNamespace();

	String getXsdTargetNamespace();

	String getResourceBase();

	IScopeProvider getScopeProvider();

	int compareToByContents(IXSD other);
}
