import { appModule } from "../../app.module";

appModule.directive('minimalizaSidebar', ['Sidebar', function (Sidebar) {
	return {
		restrict: 'A',
		template: '<a class="navbar-minimalize minimalize" href="" ng-click="toggleSidebar()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>',
		controller: function ($scope, $element) {
			$scope.toggleSidebar = function () { Sidebar.toggle() };
		}
	};
}]);
