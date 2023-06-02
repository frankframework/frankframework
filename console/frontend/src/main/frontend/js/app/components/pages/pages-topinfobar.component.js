import {appModule} from "../../app.module";

const PagesTopinfobarController = function ($scope, $state) {
	const ctrl = this;

	ctrl.loading = false;

	ctrl.$onInit = function(){
		$scope.$on('loading', function(event, loading) { ctrl.loading = loading; });
	}

	ctrl.$postLink = function () { // TEST
		// setTimeout(() => {
			console.log('TEST', $state.current.data.breadcrumbs);
		// }, 5000);
	}
}

appModule.component('pagesTopinfobar', {
	controller: ['$scope', '$state', PagesTopinfobarController],
	templateUrl: 'js/app/components/pages/pages-topinfobar.component.html',
});
