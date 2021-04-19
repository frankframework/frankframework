angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', '$rootScope', function($scope, $http, properties, $rootScope) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/frankdoc.json";
	}

	$scope.categories = {};
	$scope.elements = {};
	var http = $http.get(getURI()).then(function(response) {
		if(response && response.data) {
			var data = response.data
			var elements = data.elements;

			//map elements so we can search
			$scope.categories = data.groups;
			for(i in elements) {
				var element = elements[i];
				$scope.elements[element.fullName] = element;
			}
		}
	});
	$rootScope.aaaa = "asdfasdf";

	$scope.element = null;
	$scope.$on('element', function(_, element) {
		$scope.element = element;

		if(element != null) {
			$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + element.fullName.replaceAll(".", "/") + '.html';
		}
	});
}]).controller('sidebar', ['$scope', function($scope) {
	console.info('sidebar controller');
}]).controller('element', ['$scope', function($scope) {
	console.info('element controller', $scope.element);
}]).controller('parent-element', ['$scope', function($scope) {
	$scope.$watch('element', function(a, element) {
		console.warn(4, a, element, $scope)
	});
	$scope.$on('element', function(_, element) {
		console.warn("parent element", element);
//		updateElement(element);
	});
	console.info("parent element", $scope.element);
//	if(!$scope.element || !$scope.element.parent) return;

	function updateElement(parent) {
		console.warn($scope.elements);
		$scope.element = $scope.elements[parent];
		console.warn(parent);
		$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + $scope.element.fullName.replaceAll(".", "/") + '.html';
	}
//	updateElement($scope.parent); //First update
}]);
