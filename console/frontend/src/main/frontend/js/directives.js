import './app/page-title.directive';

import './app/components/pages/hamburger.directive';
import './app/components/pages/minimaliza-sidebar.directive';
import './app/components/pages/scroll-to-top.directive';
import './app/components/pages/side-navigation.directive';
import './app/components/icheck.directive';
import './app/components/time-since.directive';
import './app/components/to-date.directive';

import './app/views/configuration/configurations-manage/configurations-manage-details/icheck-radius.directive';
import './app/views/configuration/format-code.directive';
import './app/views/iframe/fit-height.directive';
import './app/views/jdbc/jdbc-execute-query/quick-submit-form.directive';
import './app/views/logging/clipboard.directive';
import './app/views/status/ui-lref.directive';
import './app/views/storage/back-button.directive';

angular.module('iaf.beheerconsole')
	.directive('flow', ['Misc', '$http', '$uibModal', function (Misc, $http, $uibModal) {
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
	}])

	.directive('inputFileUpload', function () {
		return {
			restrict: 'E',
			transclude: true,
			replace: true,
			link: function (scope, element) {
				element.bind("change", function () {
					scope.handleFile(this.files);
				});
				scope.handleFile = function (files) {
					if (files.length == 0) {
						scope.file = null;
						return;
					}
					scope.file = files[0]; //Can only parse 1 file!
				}
			},
			template: '<input class="form-control form-file" name="file" type="file" />'
		};
	})

	.directive('customViews', ['appConstants', function (appConstants) {
		return {
			restrict: 'E',
			replace: true,
			link: function (scope, element, attributes) {
				scope.customViews = [];
				scope.$on('appConstants', function () {
					var customViews = appConstants["customViews.names"];
					if (customViews == undefined)
						return;

					if (customViews.length > 0) {
						var views = customViews.split(",");
						for (i in views) {
							var viewId = views[i];
							var name = appConstants["customViews." + viewId + ".name"];
							var url = appConstants["customViews." + viewId + ".url"];
							if (name && url)
								scope.customViews.push({
									view: viewId,
									name: name,
									url: url
								});
						}
					}
				});
			},
			template: '<li ng-repeat="view in customViews" ui-sref-active="active">' +
				'<a ui-sref="pages.customView(view)"><i class="fa fa-desktop"></i> <span class="nav-label">{{view.name}}</span></a>' +
				'</li>'
		};
	}]);
