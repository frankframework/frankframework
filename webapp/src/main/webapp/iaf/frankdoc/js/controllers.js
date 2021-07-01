angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', function($scope, $http, properties) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/frankdoc.json";
	}

	$scope.categories = {};
	$scope.types = {};
	$scope.elements = {};
	var http = $http.get(getURI()).then(function(response) {
		if(response && response.data) {
			var data = response.data;
			var types = data.types;
			var elements = data.elements;

			//map elements so we can search
			$scope.categories = data.groups;
			for(i in types) {
				var aType = types[i];
				$scope.types[aType.name] = aType.members;
			}
			for(i in elements) {
				var element = elements[i];
				addAttributeActive(element);
				$scope.elements[element.fullName] = element;
			}
		}
	});

	function addAttributeActive(element) {
		attributeActive = {
			name: "active",
			description: "If defined and empty or false, then this element and all its children are ignored"
		};
		if(element.attributes) {
			element.attributes.unshift(attributeActive);
		} else {
			element.attributes = [attributeActive];
		}
	}

	$scope.element = null;
	$scope.$on('element', function(_, element) {
		$scope.element = element;

		if(element != null) {
			$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + element.fullName.replaceAll(".", "/") + '.html';
		}
	});
}]).controller('parent-element', ['$scope', function($scope) {
	if(!$scope.element || !$scope.element.parent) return;

	var parent = $scope.element.parent;
	$scope.element = $scope.elements[parent]; //Update element to the parent's element
	$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + $scope.element.fullName.replaceAll(".", "/") + '.html';
}]).controller('element-child-controller', ['$scope', function($scope) {
	$scope.init = function(child) {
		let childCategory = getCategoryOfType(child.type, $scope.$parent.categories);
		let childElements = $scope.getChildElements(child);
		let title = 'From ' + childCategory + ": ";
		for(i = 0; i < childElements.length; ++i) {
			if(i == 0) {
				title = title + childElements[i];
			} else {
				title = title + ", " + childElements[i];
			}
		}
		$scope.title = title;
	}

	$scope.getChildElements = function(child) {
		fullNames = $scope.$parent.types[child.type];
		simpleNames = [];
		fullNames.forEach(fullName => simpleNames.push(fullNameToSimpleName(fullName)));
		return simpleNames;
	}
}]);
