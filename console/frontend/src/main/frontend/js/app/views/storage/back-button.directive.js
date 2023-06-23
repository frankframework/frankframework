import { appModule } from "../../app.module";

appModule.directive('backButton', function () {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			element.bind('click', goBack);
			function goBack() {
				history.back();
				scope.$apply();
			}
		}
	}
});
