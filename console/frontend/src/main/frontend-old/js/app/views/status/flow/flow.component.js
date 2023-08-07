import { appModule } from "../../../app.module";

const FlowController = function (Misc, $http, $uibModal) {
	const ctrl = this;

	ctrl.$onInit = function () {
		const uri = Misc.getServerPath() + 'iaf/api/configurations/' + ctrl.adapter.configuration + '/adapters/' + Misc.escapeURL(ctrl.adapter.name) + "/flow?" + ctrl.adapter.upSince;
		ctrl.flow = { "image": null, "url": uri };
		$http.get(uri).then(function (data) {
			const status = (data && data.status) ? data.status : 204;
			if (status == 200) {
				const contentType = data.headers("Content-Type");
				ctrl.flow.image = (contentType.indexOf("image") > 0 || contentType.indexOf("svg") > 0); //display an image or a button to open a modal
				if (!ctrl.flow.image) { //only store metadata when required
					data.adapter = ctrl.adapter;
					ctrl.flow.data = data;
				}
			} else { //If non successfull response, force no-image-available
				ctrl.flow.image = true;
				ctrl.flow.url = 'images/no_image_available.svg'
			}
		});
	}

	ctrl.openFlowModal = function (xhr) {
		ctrl.flowModalLadda = true;
		$uibModal.open({
			templateUrl: 'js/app/views/status/flow/flow-modal/flow-modal.html',
			windowClass: 'mermaidFlow',
			resolve: {
				xhr: function () {
					return xhr;
				}
			},
			controller: 'FlowDiagramModalCtrl'
		});
		setTimeout(function () { ctrl.flowModalLadda = false; }, 1000);
	}
}

appModule.component('flow', {
	bindings: {
		adapter: '<'
	},
	transclude: true,
	controller: ['Misc', '$http', '$uibModal', FlowController],
	template: `<a ng-if="$ctrl.flow.image === true" ng-href="{{$ctrl.flow.url}}" target="_blank" rel="noopener noreferrer">
		<img ng-src="{{$ctrl.flow.url}}" alt="Flow Diagram">
	</a>
	<button ng-if="$ctrl.flow.image === false" ladda="$ctrl.flowModalLadda" ng-click="$ctrl.openFlowModal(flow.data)" title="Generate Flow Diagram" class="btn btn-xs btn-info" type="button"><i class="fa fa-share-alt-square"></i> Flow Diagram</button>`
})
