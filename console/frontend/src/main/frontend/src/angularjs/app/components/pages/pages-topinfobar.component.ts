import { StateService } from "@uirouter/angularjs";
import {appModule} from "../../app.module";
import { AppService } from "../../app.service";

class PagesTopinfobarController {
	loading = true;

  constructor(private appService: AppService, private $state: StateService){
    this.$state = $state; // not really needed, just to make sure nothing breaks
  }

	$onInit(){
    this.appService.loading$.subscribe(loading => this.loading = loading);
	}
}

appModule.component('pagesTopinfobar', {
	controller: ['appService', '$state', PagesTopinfobarController],
  templateUrl: 'angularjs/app/components/pages/pages-topinfobar.component.html',
});
