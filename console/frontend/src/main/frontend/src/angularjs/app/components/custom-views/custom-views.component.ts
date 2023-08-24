import { AppConstants, appModule } from "../../app.module";

class CustomViewsController {
  customViews: {
    view: string,
    name: string,
    url: string
  }[] = [];

  constructor(private $scope: angular.IScope, private appConstants: AppConstants){}

	$onInit() {
		this.$scope.$on('appConstants', () => {
			var customViews = this.appConstants["customViews.names"];
			if (customViews == undefined)
				return;

			if (customViews.length > 0) {
				var views = customViews.split(",");
				for (const i in views) {
					var viewId = views[i];
					var name = this.appConstants["customViews." + viewId + ".name"];
					var url = this.appConstants["customViews." + viewId + ".url"];
					if (name && url) this.customViews.push({
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
	template: '<li ng-repeat="view in customViews" ui-sref-active="active">' +
		'<a ui-sref="pages.customView(view)"><i class="fa fa-desktop"></i> <span class="nav-label">{{view.name}}</span></a>' +
		'</li>'
});
