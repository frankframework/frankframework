import { appModule } from "../../../app.module";

appModule.controller('InformationCtrl', ['$scope', '$uibModalInstance', '$uibModal', 'Api', '$timeout', function ($scope, $uibModalInstance, $uibModal, Api, $timeout) {
	$scope.error = false;
	Api.Get("server/info", function (data) {
		$.extend($scope, data);
	}, function () {
		$scope.error = true;
	});

	$scope.close = function () {
		$uibModalInstance.close();
	};

	$scope.openCookieModel = function () {
		$uibModalInstance.close(); //close the current model

		$timeout(function () {
			$uibModal.open({
				templateUrl: 'js/app/components/pages/information-modal/cookieModal.html',
				size: 'lg',
				backdrop: 'static',
				controller: 'CookieModalCtrl',
			});
		});
	}
}]);
