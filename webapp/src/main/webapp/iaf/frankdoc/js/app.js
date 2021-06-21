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
		controller: function($scope, $state) {
			$state.go("group", {group: 'All'});
		}
	})
	.state('group', {
		url: "/:group",
		params: {
			group: { value: 'All' },
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
			let groupName = $state.params.group;
			$scope.$watch('groups', function(groups) {
				if(!groups || groups.length < 1) return;

				for(i in groups) {
					let group = $scope.groups[i];
					if(group.name == groupName) {
						$rootScope.group = group;
						break;
					}
				}

				if($scope.group && $state.params && $state.params.element) {
					let elementSimpleName = $state.params.element;
					// Match the SimpleName of the Frank!Element and try and find it in the group's members
					let groupMembers = getGroupMembers($scope.types, $scope.group.types);
					for(i in groupMembers) {
						let memberName = groupMembers[i];
						if($scope.elements[memberName].name == elementSimpleName) {
							$rootScope.$broadcast('element', $scope.elements[memberName]);
						}
					}
				} else {
					$rootScope.$broadcast('element', null);
				}
			}); //Fired once, when API call has been completed
		},
	})
	.state('element', {
		parent: "group",
		url: "/:element",
	});
}])
.filter('matchElement', function() {
	return function(elements, $scope) {
		if(!elements || elements.length < 1 || !$scope.group) return []; //Cannot filter elements if no group has been selected
		let r = {};
		let groupMembers = getGroupMembers($scope.types, $scope.group.types);
		for(element in elements) {
			if(groupMembers.indexOf(element) > -1) {
				r[element] = elements[element];
			}
		}
		return r;
	};
});

function getGroupMembers(allTypes, typesToFilterOn) {
	let memberNames = [];
	typesToFilterOn.forEach(t => memberNames = memberNames.concat(allTypes[t])); //Find all members in the supplied type(s)
	return memberNames.filter((x, i, a) => a.indexOf(x) == i); //get distinct array results
}

function getGroupsOfType(type, groups) {
	for(i = 0; i < groups.length; ++i) {
		let group = groups[i];
		if(group.types.indexOf(type) >= 0) {
			return group.name;
		}
	}
	return null;
}
