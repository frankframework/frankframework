import { appModule } from "../../app.module";

appModule.component('pagesNavigation', {
	bindings: {
		onOpenInfo: '&',
		onOpenFeedback: '&'
	},
	templateUrl: 'js/app/components/pages/pages-navigation.component.html'
});
