import { appModule } from "../../app.module";

appModule.directive('hamburger', ['Sidebar', function (Sidebar) {
	return {
		restrict: 'A',
		template: '<a class="hamburger btn btn-primary " href="" ng-click="toggleSidebar()"><i class="fa fa-bars"></i></a>',
		controller: function ($scope, $element) {
			$scope.toggleSidebar = function () { Sidebar.toggle() };
		}
	};
}]);
