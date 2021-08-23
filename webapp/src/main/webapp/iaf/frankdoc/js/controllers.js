angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', function($scope, $http, properties) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/";
	}
	$scope.showDeprecatedElements = false;
	$scope.showHideDeprecated = function() {
		$scope.showDeprecatedElements = !$scope.showDeprecatedElements;
	}
	$scope.downloadXSD = function() {
		window.open(getURI() + "frankdoc.xsd", 'Frank!Doc XSD');
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
			$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + element.fullName.replaceAll(".", "/") + '.html';
		}
	});
}]).controller('parent-element', ['$scope', function($scope) {
	if(!$scope.element || !$scope.element.parent) return;

	var parent = $scope.element.parent;
	$scope.element = $scope.elements[parent]; //Update element to the parent's element
	$scope.javaDocURL = 'https://javadoc.ibissource.org/latest/' + $scope.element.fullName.replaceAll(".", "/") + '.html';
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
		fullNames.forEach(fullName => simpleNames.push(fullNameToSimpleName(fullName)));
		return simpleNames;
	}
}]);
