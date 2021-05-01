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
		data: {
			pageTitle: 'Overview'
		}
	})
	.state('category', {
		url: "/:category",
		params: {
			category: { value: '', squash: true},
			element: { value: '', squash: true},
		},
		templateUrl: function($scope) {
			if($scope.element) {
				return "views/element.html";
			} else {
				return "views/overview.html";
			}
		},
		controller: function($scope, $state, $rootScope) {
			var categoryName = $state.params.category;
			$scope.$watch('categories', function(categories) {
				if(!categories || categories.length < 1) return;

				for(i in categories) {
					var category = $scope.categories[i];
					if(category.name == categoryName) {
						$rootScope.category = category;
					}
				}

				if($scope.category && $state.params && $state.params.element) {
					var elementName = $state.params.element;
					for(i in $scope.category.members) {
						var fullName = $scope.category.members[i];
						if($scope.elements[fullName].name == elementName) {
							$rootScope.$broadcast('element', $scope.elements[fullName]);
						}
					}
				} else {
					$rootScope.$broadcast('element', null);
				}
			}); //Fired once, when API call has been completed
		},
		data: {
			pageTitle: 'Overview'
		}
	})
	.state('element', {
		parent: "category",
		url: "/:element",
		data: {
			pageTitle: 'Overview'
		}
	});
}])
.filter('matchElement', function() {
	return function(elements, $scope) {
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