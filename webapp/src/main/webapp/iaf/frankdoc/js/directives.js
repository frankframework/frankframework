angular.module('iaf.frankdoc')
.directive('pageTitle', ['$timeout', '$state', '$transitions', function($timeout, $state, $transitions) {
	return {
		link: function(scope, element) {
			var listener = function() {
				let title = 'Frank!Doc';
				$timeout(function() {
					element.text(title);
				});
			};
			$transitions.onSuccess({}, listener); //Fired on every state change
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
		templateUrl: 'views/element-children.html'
	}
}]).directive('elementAttributes', [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: 'views/element-attributes.html'
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
}]);