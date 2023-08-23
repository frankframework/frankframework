import { StateService } from "@uirouter/angularjs";
import { appModule } from "../../app.module";

class PagesNavigationController {
  constructor(private $state: StateService){
    this.$state = $state; // not really needed, just to make sure nothing breaks
  }
}

appModule.component('pagesNavigation', {
	bindings: {
		onOpenInfo: '&',
		onOpenFeedback: '&'
	},
	controller: ['$state', PagesNavigationController],
  templateUrl: 'angularjs/app/components/pages/pages-navigation.component.html'
});
