import { appModule } from "../../../../app.module";

appModule.controller('FlowDiagramModalCtrl', ['$scope', '$uibModalInstance', 'xhr', function ($scope, $uibModalInstance, xhr) {
	$scope.adapter = xhr.adapter;
	$scope.flow = xhr.data;

	$scope.close = function () {
		$uibModalInstance.close();
	};
}]);
