angular.module('iaf.frankdoc').directive('overview', [function() {
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
}]);