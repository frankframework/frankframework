import {appModule} from "../../app.module";

const PagesTopinfobarController = function($scope) {
	const ctrl = this;

	ctrl.loading = false;

	ctrl.$onInit = function(){
		$scope.$on('loading', function(event, loading) { ctrl.loading = loading; });
	}
}

appModule.component('pagesTopinfobar', {
	controller: ['$scope', PagesTopinfobarController],
	templateUrl: 'js/app/components/pages/pages-topinfobar.component.html',
});
