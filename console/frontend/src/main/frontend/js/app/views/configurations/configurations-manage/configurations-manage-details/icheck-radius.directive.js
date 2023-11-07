import { appModule } from "../../../../app.module";

appModule.directive('icheckRadius', ['$timeout', '$parse', function ($timeout, $parse) {
	return {
		restrict: 'A',
		require: 'ngModel',
		link: function ($scope, element, $attrs, ngModel) {
			return $timeout(function () {

				$scope.$watch($attrs['ngModel'], function (newValue) {
					$(element).iCheck('update');
				});

				return $(element).iCheck({
					checkboxClass: 'iradio_square-green',
				}).on('ifChanged', function (event) {
					if ($(element).attr('type') === 'checkbox' && $attrs['ngModel']) {
						$scope.$apply(function () {
							return ngModel.$setViewValue(event.target.checked);
						});
					}
				});
			});
		}
	};
}]);
