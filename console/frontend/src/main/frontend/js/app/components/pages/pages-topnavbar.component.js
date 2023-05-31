import { appModule } from "../../app.module";

appModule.component('pagesTopnavbar', {
	bindings: {
		onOpenFeedback: '&'
	},
	templateUrl: 'js/app/components/pages/pages-topnavbar.component.html'
});
