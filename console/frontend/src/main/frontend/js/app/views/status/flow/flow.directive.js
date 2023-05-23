import { appModule } from "../../../app.module";

// !! Element directive !!
appModule.directive('flow', ['Misc', '$http', '$uibModal', function (Misc, $http, $uibModal) {
	return {
		restrict: 'E',
		transclude: true,
		scope: {
			adapter: '='
		},
		link: function (scope) {
			let adapter = scope.adapter;
			let uri = Misc.getServerPath() + 'iaf/api/configurations/' + adapter.configuration + '/adapters/' + Misc.escapeURL(adapter.name) + "/flow?" + adapter.upSince;
			scope.flow = { "image": null, "url": uri };
			$http.get(uri).then(function (data) {
				let status = (data && data.status) ? data.status : 204;
				if (status == 200) {
					let contentType = data.headers("Content-Type");
					scope.flow.image = (contentType.indexOf("image") > 0 || contentType.indexOf("svg") > 0); //display an image or a button to open a modal
					if (!scope.flow.image) { //only store metadata when required
						data.adapter = adapter;
						scope.flow.data = data;
					}
				} else { //If non successfull response, force no-image-available
					scope.flow.image = true;
					scope.flow.url = 'images/no_image_available.svg'
				}
			});

			scope.openFlowModal = function (xhr) {
				scope.flowModalLadda = true;
				$uibModal.open({
					templateUrl: 'views/flow-modal.html',
					windowClass: 'mermaidFlow',
					resolve: {
						xhr: function () {
							return xhr;
						}
					},
					controller: 'FlowDiagramModalCtrl'
				});
				setTimeout(function () { scope.flowModalLadda = false; }, 1000);
			}
		},
		template: '<a ng-if="flow.image === true" ng-href="{{flow.url}}" target="_blank"><img ng-src="{{flow.url}}" alt="Flow Diagram"></a><button ng-if="flow.image === false" ladda="flowModalLadda" ng-click="openFlowModal(flow.data)" title="Generate Flow Diagram" class="btn btn-xs btn-info" type="button"><i class="fa fa-share-alt-square"></i> Flow Diagram</button>'
	}
}]);
