import { appModule } from "../../app.module";

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
