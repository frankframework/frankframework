import { appModule } from "../../app.module";

appModule.directive('scrollToTop', function () {
	return {
		restrict: 'A',
		replace: true,
		template: '<div class="scroll-to-top"><a title="Scroll to top" ng-click="scrollTop()"><i class="fa fa-arrow-up"></i> <span class="nav-label">Scroll To Top</span></a></div>',
		controller: function ($scope) {
			$scope.scrollTop = function () {
				$(window).scrollTop(0);
			};
		}
	};
});
