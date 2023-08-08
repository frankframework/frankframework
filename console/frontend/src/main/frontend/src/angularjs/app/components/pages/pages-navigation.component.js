import { appModule } from "../../app.module";

const PagesNavigationController = function ($state) {
	const ctrl = this;
	ctrl.$state = $state;
}

appModule.component('pagesNavigation', {
	bindings: {
		onOpenInfo: '&',
		onOpenFeedback: '&'
	},
	controller: ['$state', PagesNavigationController],
  templateUrl: 'angularjs/app/components/pages/pages-navigation.component.html'
});
