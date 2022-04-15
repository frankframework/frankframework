angular.module('iaf.frankdoc')
.directive('pageTitle', ['$timeout', function($timeout) {
	return {
		link: function(scope, el) {
			scope.$on('element', function(_, element) {
				let title = 'Frank!Doc';
				if(element) {
					title += ' - '+element.name;
				}
				$timeout(function() {
					el.text(title);
				});
			});
		}
	};
}]).directive('overview', [function() {
	return {
		restrict: 'E',
		replace: true,
		templateUrl: 'views/overview.html'
	};
}]).directive('sidebar', [function() {
	return {
		restrict: 'E',
		replace: true,
		templateUrl: 'views/sidebar.html'
	};
}]).directive('sidebarElements', [function() {
	return {
		restrict: 'E',
		transclude: true,
		replace: true,
		templateUrl: 'views/sidebar-elements.html'
	};
}]).directive('parentElement', [function() {
	return {
		restrict: 'E',
		replace: true,
		controller: 'parent-element',
		templateUrl: 'views/parent-element.html'
	};
}]).directive('elementChildren', [function() {
	return {
		restrict: 'A',
		replace: true,
		controller: 'element-children',
		templateUrl: 'views/element-children.html'
	}
}]).directive('elementAttributes', [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: 'views/element-attributes.html'
	};
}]).directive('elementForwards', [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: 'views/element-forwards.html'
	};
}]).directive("suppressAttributeInheritance", [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: "views/suppress-attribute-inheritance.html"
	}
}]).directive("attributeDescription", [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: "views/attribute-description.html"
	}
}]).directive("parameters", [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: "views/element-parameters.html"
	}
}]);