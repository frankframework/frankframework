import { appModule } from "../../app.module";

function parseStateRef(ref, current) {
	var preparsed = ref.match(/^\s*({[^}]*})\s*$/), parsed;
	if (preparsed) ref = current + '(' + preparsed[1] + ')';
	parsed = ref.replace(/\n/g, " ").match(/^([^(]+?)\s*(\((.*)\))?$/);
	if (!parsed || parsed.length !== 4) throw new Error("Invalid state ref '" + ref + "'");
	return { state: parsed[1], paramExpr: parsed[3] || null };
}

appModule.directive('uiLref', ['$state', '$location', '$timeout', function ($state, $location, $timeout) {
	return {
		link: function (scope, element, attributes) {
			var ref = parseStateRef(attributes.uiLref, $state.current.name);
			var params;
			if (ref.paramExpr) {
				params = angular.copy(scope.$eval(ref.paramExpr));
			}

			var transition = null;
			element.bind("click", function () {
				if (transition) {
					$timeout.cancel(transition);
				}
				var adapter = scope.adapter;
				if (adapter) {
					$timeout(function () {
						$location.hash(adapter.name);
					});
				}
				transition = $timeout(function () {
					$state.go(ref.state, params);
				}, 5);
			});
		}
	};
}]);
