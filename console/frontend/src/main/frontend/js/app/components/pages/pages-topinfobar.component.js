import {appModule} from "../../app.module";

const PagesTopinfobarController = function ($scope, $state) {
	const ctrl = this;

	ctrl.$state = $state;
	ctrl.loading = false;

	ctrl.$onInit = function(){
		$scope.$on('loading', function(event, loading) { ctrl.loading = loading; });
	}
}

appModule.component('pagesTopinfobar', {
	controller: ['$scope', '$state', PagesTopinfobarController],
	templateUrl: 'js/app/components/pages/pages-topinfobar.component.html',
});
