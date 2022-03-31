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
							let el = $scope.elements[memberName];
							if($scope.showInheritance) {
								el = $scope.flattenElements(el);
							}
							$rootScope.$broadcast('element', el);
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
	return function(elements, $scope, searchText) {
		let searchTextLC = null;
		if(searchText && searchText != "") {
			searchTextLC = searchText.toLowerCase();
		}
		if(!elements || elements.length < 1 || !$scope.group) return []; //Cannot filter elements if no group has been selected
		let r = {};
		let groupMembers = getGroupMembers($scope.types, $scope.group.types);
		for(element in elements) {
			if(groupMembers.indexOf(element) > -1) {
				let obj = elements[element];
				if(searchTextLC) {
					if(JSON.stringify(obj).replace(/"/g, '').toLowerCase().indexOf(searchTextLC) > -1) {
						r[element] = obj;
					}
				} else {
					r[element] = obj;
				}
			}
		}
		return r;
	};
}).filter('javadoc', function($sce) {
	return function(input, $scope) {
		if(!input || !$scope.elements) return;
		input = input.replace(/\[(.*?)\]\((.+?)\)/g, '<a target="_blank" href="$2" alt="$1">$1</a>');
		input = input.replaceAll('\\"', '"');
		input = input.replace(/(?:{@link\s(.*?)})/g, function(match, captureGroup) {
			// {@link PipeLineSession pipeLineSession}
			// {@link IPipe#configure()}
			// {@link #doPipe(Message, PipeLineSession) doPipe}
			let referencedElement = captureGroup;
			let hash = captureGroup.indexOf("#");
			if(hash > -1) {
				referencedElement = captureGroup.split("#")[0];

				if(referencedElement == '') { //if there is no element ref then it's a method
					let method = captureGroup.substring(hash);
					let nameOrAlias = method.split(") ");
					if(nameOrAlias.length == 2) {
						return nameOrAlias[1]; //If it's an alias
					}
					return method.substring(1, method.indexOf("("));
				}
			}
			let captures = referencedElement.split(" ");
			let name = captures[captures.length-1];
			if(hash > -1) {
				let method = captureGroup.split("#")[1];
				name = name +"."+ (method.substring(method.indexOf(") ")+1)).trim();
			}
			let element = findElement($scope.elements, captures[0]);
			if(!element) {
				return name;
			}

			return '<a href="#!/All/'+element.name+'">'+name+'</a>';
		});

		return $sce.trustAsHtml(input);
	};
});

function findElement(allElements, simpleName) {
	if(!allElements || allElements.length < 1) return null; //Cannot find anything if we have nothing to search in
	let arr = [];
	for(element in allElements) {
		if(fullNameToSimpleName(element) == simpleName) {
			arr.push(allElements[element]);
		}
	}
	if(arr.length == 1) {
		return arr[0];
	}

	if(arr.length == 0) {
		console.warn("could not find element ["+simpleName+"]");
	} else {
		console.warn("found multiple elements, playing safe, returning null", arr);
	}
	return null;
}

function fullNameToSimpleName(fullName) {
	return fullName.substr(fullName.lastIndexOf(".")+1)
}

function getGroupMembers(allTypes, typesToFilterOn) {
	let memberNames = [];
	typesToFilterOn.forEach(t => memberNames = memberNames.concat(allTypes[t])); //Find all members in the supplied type(s)
	return memberNames.filter((x, i, a) => a.indexOf(x) == i); //get distinct array results
}

// Exclude group All.
function getGroupsOfType(type, groups) {
	for(i = 1; i < groups.length; ++i) {
		let group = groups[i];
		if(group.types.indexOf(type) >= 0) {
			return group.name;
		}
	}
	return null;
}
