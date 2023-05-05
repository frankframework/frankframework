import {appModule} from "../../../app.module";

appModule.controller('InfoBarCtrl', ['$scope', function($scope) {
	$scope.$on('loading', function(event, loading) { $scope.loading = loading; });
}]);
