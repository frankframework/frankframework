import { appModule } from "./app.module";

appModule.directive('pageTitle', ['$rootScope', '$timeout', '$state', '$transitions', 'Debug', function ($rootScope, $timeout, $state, $transitions, Debug) {
	return {
		link: function (scope, element) {
			var listener = function () {
				var toState = $state.current;
				Debug.info("state change", toState);

				var title = 'Loading...'; // Default title
				if (toState.data && toState.data.pageTitle && $rootScope.instanceName) title = $rootScope.dtapStage + '-' + $rootScope.instanceName + ' | ' + toState.data.pageTitle;
				else if ($rootScope.startupError) title = "ERROR";
				$timeout(function () {
					element.text(title);
				});
			};
			$transitions.onSuccess({}, listener); //Fired on every state change
			$rootScope.$watch('::instanceName', listener); //Fired once, once the instance name is known.
		}
	};
}]);
