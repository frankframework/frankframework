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
	console.log('Created element-child-controller');

	$scope.visible = false;

	$scope.init = function(child) {
		$scope.child = child;
		$scope.childCategory = getCategoryOfType(child.type, $scope.$parent);
	}

	$scope.onClick = function() {
		console.log('Button clicked for child' + $scope.child.type);
		$scope.visible = ! $scope.visible;
	}

	$scope.getChildElements = function() {
		fullNames = $scope.$parent.types[$scope.child.type];
		simpleNames = [];
		fullNames.forEach(fullName => simpleNames.push(fullNameToSimpleName(fullName)));
		return simpleNames;
	}
}]);
