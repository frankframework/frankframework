import { appModule } from "../../app.module";

appModule.directive('fitHeight', function () {
	return {
		restrict: 'A',
		link: function ($scope, element) {
			$scope.height = {
				topnavbar: 0,
				topinfobar: 0,
				window: 0,
				min: 800
			};

			function fitHeight() {
				var offset = $scope.height.topnavbar + $scope.height.topinfobar;
				var height = ($scope.height.window > $scope.height.min ? $scope.height.window : $scope.height.min) - offset;
				element.css("height", height + "px");
				element.css("min-height", height + "px");
			}

			$scope.$watch(function () { return $(window).height(); }, function (newValue) {
				if (!newValue) return;
				$scope.height.window = newValue;
				fitHeight();
			});
			$scope.$watch(function () { return $('nav.navbar-default').height(); }, function (newValue) {
				if (!newValue) return;
				$scope.height.min = newValue;
				fitHeight();
			});
			$scope.$watch(function () { return $('.topnavbar').height(); }, function (newValue) {
				if (!newValue) return;
				$scope.height.topnavbar = newValue;
				fitHeight();
			});
			$scope.$watch(function () { return $('.topinfobar').height(); }, function (newValue) {
				if (!newValue) return;
				$scope.height.topinfobar = newValue;
				fitHeight();
			});

			fitHeight();
		}
	};
});
