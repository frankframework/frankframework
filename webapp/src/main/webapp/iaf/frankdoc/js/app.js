(function () {
	console.time("startup");
	var server; //Try and see if serverurl has been defined, if not try to deduct from local url;
	try {
		server = serverurl;
	}
	catch(e) {
		var path = window.location.pathname;

		if(path.indexOf("/iaf/frankdoc") >= 0)
			path = path.substr(0, path.indexOf("/iaf/frankdoc")+1);
		else
			if(path.indexOf("/", 1) >= 0)
				path = path.substr(0, path.indexOf("/", 1)+1);
		server = path;
	}
	angular.module('iaf.frankdoc', [
		'ui.bootstrap',                 // Ui Bootstrap
		'ui.router',                    // Routing
		'ngSanitize',                   // ngSanitize
	]).constant("properties", {
		"server": server,
	});
	console.timeEnd("startup");
})();


angular.module('iaf.frankdoc').config(['$stateProvider', '$urlRouterProvider', function config($stateProvider, $urlRouterProvider) {

	$urlRouterProvider.otherwise("/");

	$stateProvider
	.state('overview', {
		url: "/",
		templateUrl: "views/overview.html",
//		controller: 'LoginCtrl',
		data: {
			pageTitle: 'Overview'
		}
	})
	.state('category', {
		url: "/:category",
		params: {
			category: { value: '', squash: true},
		},
		templateUrl: "views/category-sub-menu.html",
		controller: function($scope, $state) {
			var categoryName = $state.params.category;
			$scope.$watch('categories', function(categories) {
				if(!categories || categories.length < 1) return;

				for(i in categories) {
					var category = $scope.categories[i];
					if(category.name == categoryName) {
						$scope.category = category;
					}
				}
			}); //Fired once, when API call has been completed
			console.log(5 , $state.params.element)
		},
		data: {
			pageTitle: 'Overview'
		}
	})
	.state('element', {
		parent: "category",
		url: "/a/:element",
		params: {
			element: { value: '', squash: true},
		},
		controller: function($rootScope, $state) {
			console.log(5 , $state.params.element)
			$rootScope.$broadcast('element', $state.params.element);
		},
		data: {
			pageTitle: 'Overview'
		}
	});
}]);

angular.module('iaf.frankdoc').controller("main", ['$scope', '$http', 'properties', function($scope, $http, properties) {
	function getURI() {
		return properties.server + "iaf/api/frankdoc/files/frankdoc.json";
	}
	$scope.categories = {};
	$scope.elements = {};
	var http = $http.get(getURI()).then(function(response) {
		if(response && response.data) {
			console.log(response)
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
}])
.filter('matchElement', function() {
	return function(elements, $scope, a, b) {
		if(!elements || elements.length < 1 || !$scope.category) return [];
		var r = {};
		var members = $scope.category.members;
		for(i in members) {
			var member = members[i];
			r[member] = elements[member];
		}
		return r;
	};
});

angular.module('iaf.frankdoc').directive('sidebar', ['$rootScope', function($rootScope) {
	return {
		restrict: 'E',
		replace: true,
		link: function(scope, element, attributes) {
		},
		controller: 'sidebar',
		templateUrl: 'views/sidebar.html'
	};
}]).directive('content', ['$rootScope', function($rootScope) {
	return {
		restrict: 'E',
		replace: true,
		link: function(scope, element, attributes) {
			console.error(element);
		},
		controller: 'content',
		templateUrl: 'views/content.html'
	};
}]);

angular.module('iaf.frankdoc').controller('sidebar', ['$scope', function($scope) {
	console.info('sidebar controller');
}]).controller('content', ['$scope', function($scope) {
	$scope.$on('element', function(element) {
		console.log(element)
	});
}]);
