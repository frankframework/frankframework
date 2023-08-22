import { appModule } from "../../app.module";
import { SidebarService } from "./sidebar.service";

class MinimalizaSidebarController {
  constructor(private Sidebar: SidebarService){}
	toggleSidebar() { this.Sidebar.toggle() };
};

appModule.component('minimalizaSidebar', {
	controller: ['Sidebar', MinimalizaSidebarController],
	template: '<a class="navbar-minimalize minimalize" href="" ng-click="$ctrl.toggleSidebar()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>'
});
