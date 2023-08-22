import { appModule } from "../../app.module";
import { SidebarService } from "./sidebar.service";

class HamburgerController {
  constructor(private Sidebar: SidebarService){}
	toggleSidebar() { this.Sidebar.toggle() };
}

appModule.component('hamburger', {
	controller: ['Sidebar', HamburgerController],
	template: '<a class="hamburger btn btn-primary " href="" ng-click="$ctrl.toggleSidebar()"><i class="fa fa-bars"></i></a>',
});
