angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', function($scope, $http, properties) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/frankdoc.json";
	}

	$scope.groups = {};
	$scope.types = {};
	$scope.elements = {};
	var http = $http.get(getURI()).then(function(response) {
		if(response && response.data) {
			let data = response.data;
			let types = data.types;
			let elements = data.elements;

			//map elements so we can search
			$scope.groups = data.groups;
			for(i in types) {
				let aType = types[i];
				$scope.types[aType.name] = aType.members;
			}
			for(i in elements) {
				let element = elements[i];
				$scope.elements[element.fullName] = element;
			}
		}
	});

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
	$scope.getTitle = function(child) {
		let childCategory = getGroupsOfType(child.type, $scope.groups);
		let childElements = $scope.getChildElements(child);
		let title = 'From ' + childCategory + ": ";
		for(i = 0; i < childElements.length; ++i) {
			if(i == 0) {
				title = title + childElements[i];
			} else {
				title = title + ", " + childElements[i];
			}
		}
		return title;
	}

	function fullNameToSimpleName(fullName) {
		return fullName.substr(fullName.lastIndexOf(".")+1)
	}
	$scope.getChildElements = function(child) {
		let fullNames = $scope.types[child.type];
		let simpleNames = [];
		fullNames.forEach(fullName => simpleNames.push(fullNameToSimpleName(fullName)));
		return simpleNames;
	}
}]);
