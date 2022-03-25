angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', function($scope, $http, properties) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/";
	}
	$scope.showDeprecatedElements = false;
	$scope.showHideDeprecated = function() {
		$scope.showDeprecatedElements = !$scope.showDeprecatedElements;
	}
	$scope.downloadXSD = function() {
		window.open(getURI() + "FrankConfig.xsd", 'Frank!Config XSD');
	}

	$scope.showInheritance = true;
	$scope.showHideInheritance = function() {
		$scope.showInheritance = !$scope.showInheritance;

		if($scope.element) {
			if($scope.showInheritance) {
				$scope.element = $scope.flattenElements($scope.element); // Merge inherited elements
			} else {
				$scope.element = $scope.elements[$scope.element.fullName]; // Update the element to it's original state
			}
		}
	}
	$scope.flattenElements = function(element) {
		if(element.parent) {
			let el = angular.copy(element);
			let parent = $scope.elements[element.parent];

			//Add separator where attributes inherit from
			if(parent.attributes && parent.attributes.length > 0) {
				if(!el.attributes) { el.attributes = []; } //Make sure an array exists
				el.attributes.push({from: parent.name});
			}

			el.attributes = copyOf(el.attributes, parent.attributes, 'name');
			el.children = copyOf(el.children, parent.children, 'roleName');
			el.forwards = copyOf(el.forwards, parent.forwards, 'name');

			if(!el.parametersDescription && parent.parametersDescription) {
				el.parametersDescription = parent.parametersDescription;
			}
			if(parent.parent) {
				el.parent = parent.parent;
			} else {
				el.parent = null;
			}
			return $scope.flattenElements(el);
		}

		return element;
	}

	$scope.groups = {};
	$scope.types = {};
	$scope.elements = {};
	$scope.enums = {};
	$scope.search = "";
	$http.get(getURI() + "frankdoc.json").then(function(response) {
		if(response && response.data) {
			let data = response.data;
			let types = data.types;
			let elements = data.elements;
			let enums = data.enums;

			//map elements so we can search
			$scope.groups = data.groups;
			let distinctTypes = [];
			types.forEach((e) => distinctTypes.push(e.name));
			$scope.groups.unshift({
				name: "All",
				types: distinctTypes
			});

			for(let i in types) {
				let aType = types[i];
				$scope.types[aType.name] = aType.members;
			}
			for(let i in elements) {
				let element = elements[i];
				$scope.elements[element.fullName] = element;
			}
			for(let i in enums) {
				let en = enums[i];
				$scope.enums[en.name] = en.values;
			}
		}
	}, function(response) {
		if(response.data && response.data.error) {
			$scope.loadError = response.data.error;
		} else {
			$scope.loadError = "Unable to load Frank!Doc.json file.";
		}
	});

	$scope.element = null;
	$scope.$on('element', function(_, element) {
		$scope.element = element;

		if(element != null) {
			$scope.javaDocURL = javaDocUrlOf(element);
		}
	});
}])
.controller('parent-element', ['$scope', function($scope) {
	if(!$scope.element || !$scope.element.parent) return;

	var parent = $scope.element.parent;
	$scope.element = $scope.elements[parent]; //Update element to the parent's element
	$scope.javaDocURL = javaDocUrlOf($scope.element);
}]).controller('element-children', ['$scope', function($scope) {
	$scope.getTitle = function(child) {
		let title = '';
		if(child.type) {
			let groups = getGroupsOfType(child.type, $scope.groups);
			let childElements = $scope.getElementsOfType(child.type);
			title = 'From ' + groups + ": ";
			for(let i = 0; i < childElements.length; ++i) {
				if(i == 0) {
					title = title + childElements[i];
				} else {
					title = title + ", " + childElements[i];
				}
			}
		} else{
			title = 'No child elements, only text';
		}
		return title;
	}

	$scope.getElementsOfType = function(type) {
		let fullNames = $scope.types[type];
		let simpleNames = [];
		if($scope.showDeprecatedElements) {
			fullNames.forEach(fullName => simpleNames.push(fullNameToSimpleName(fullName)));
		} else {
			fullNames.forEach(function(fullName) {
				if(!$scope.elements[fullName].deprecated) {
					simpleNames.push(fullNameToSimpleName(fullName))
				}
			});
		}
		return simpleNames;
	}
}]).controller('attribute-description', ['$scope', function($scope) {
	let enumFields = $scope.enums[$scope.attr.enum];
	$scope.descriptiveEnum = false; //has at least 1 enum field with a description
	for(let i in enumFields) {
		let field = enumFields[i];
		if(field.description != undefined) {
			$scope.descriptiveEnum = true;
			break;
		}
	}
}]);

function javaDocUrlOf(element) {
	if(element.fullName && element.fullName.includes(".")) {
		return 'https://javadoc.frankframework.org/' + element.fullName.replaceAll(".", "/") + '.html'	
	} else {
		// We only have a JavaDoc URL if we have an element with a Java class. The
		// exception we handle here is <Module>.
		return null;
	}
}
function copyOf(attr1, attr2, fieldName) {
	if(attr1 && !attr2) {
		return attr1;
	} else if(attr2 && !attr1) {
		return attr2;
	} else if(!attr1 && !attr2) {
		return null;
	} else {
		let newAttr = [];
		let seen = [];
		for(i in attr1) {
			let at = attr1[i];
			seen.push(at[fieldName])
			newAttr.push(at);
		}
		for(i in attr2) {
			let at = attr2[i];
			if(seen.indexOf(at[fieldName]) === -1) {
				newAttr.push(at);
			}
		}
		return newAttr;
	}
}