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
		controller: 'sidebar',
		templateUrl: 'views/sidebar.html'
	};
}]).directive('content', [function() {
	return {
		restrict: 'E',
		replace: true,
		controller: 'element',
		templateUrl: 'views/element.html'
	};
}]).directive('parentElement', [function() {
	return {
		restrict: 'A',
		replace: true,
		controller: 'parent-element',
		templateUrl: 'views/parent-element.html'
	};
}]).directive('elementAttributes', [function() {
	return {
		restrict: 'A',
		replace: true,
		templateUrl: 'views/element-attributes.html'
	};
}]);