import { appModule } from "../../app.module";

appModule.directive('sideNavigation', ['$timeout', function ($timeout) {
	return {
		restrict: 'A',
		link: function (scope, element) {
			// Call the metisMenu plugin and plug it to sidebar navigation
			$timeout(function () {
				element.metisMenu();

			});
		}
	};
}]);
