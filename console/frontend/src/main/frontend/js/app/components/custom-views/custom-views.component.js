import { appModule } from "../../app.module";

const CustomViewsController = function ($scope, appConstants) {
	const ctrl = this;

	ctrl.customViews = [];

	ctrl.$onInit = function () {
		$scope.$on('appConstants', function () {
			let customViews = appConstants["customViews.names"];
			if (customViews == undefined)
				return;

			if (customViews.length > 0) {
				let views = customViews.split(",");
				for (let i in views) {
					let viewId = views[i];
					let name = appConstants["customViews." + viewId + ".name"];
					let url = appConstants["customViews." + viewId + ".url"];
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
	controller: ['$scope', 'appConstants', CustomViewsController],
	template: `<ul class="nav">
		<li ng-repeat="view in $ctrl.customViews" ui-sref-active="active">
			<a ui-sref="pages.customView(view)"><i class="fa fa-desktop"></i> <span class="nav-label">{{view.name}}</span></a>
		</li>
	</ul>`
});
