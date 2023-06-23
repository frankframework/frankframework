import { appModule } from "../../app.module";

appModule.controller('NotificationsCtrl', ['$scope', 'Api', '$stateParams', 'Hooks', 'Notification',
	function ($scope, Api, $stateParams, Hooks, Notification) {
		if ($stateParams.id > 0) {
			$scope.notification = Notification.get($stateParams.id);
		}
		else {
			$scope.text = ("Showing a list with all notifications!");
		}

		Hooks.register("adapterUpdated:2", function (adapter) {
			console.warn("What is the scope of: ", adapter);
		});
	}]);
