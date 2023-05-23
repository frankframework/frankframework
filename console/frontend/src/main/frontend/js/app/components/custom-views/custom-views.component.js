import { appModule } from "../../app.module";

const CustomViewsController = function ($scope) {
	const ctrl = this;

	ctrl.customViews = [];

	ctrl.$onInit = function () {
		$scope.$on('appConstants', function () {
			var customViews = appConstants["customViews.names"];
			if (customViews == undefined)
				return;

			if (customViews.length > 0) {
				var views = customViews.split(",");
				for (i in views) {
					var viewId = views[i];
					var name = appConstants["customViews." + viewId + ".name"];
					var url = appConstants["customViews." + viewId + ".url"];
					if (name && url) ctrl.customViews.push({
						view: viewId,
						name: name,
						url: url
					});
				}
			}
		});
	}
}

appModule.component('customViews', {
	controller: ['$scope', CustomViewsController],
	template: '<li ng-repeat="view in customViews" ui-sref-active="active">' +
		'<a ui-sref="pages.customView(view)"><i class="fa fa-desktop"></i> <span class="nav-label">{{view.name}}</span></a>' +
		'</li>'
});
