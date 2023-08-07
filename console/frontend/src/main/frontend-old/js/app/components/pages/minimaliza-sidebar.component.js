import { appModule } from "../../app.module";

const MinimalizaSidebarController = function (Sidebar) {
	const ctrl = this;
	ctrl.toggleSidebar = function () { Sidebar.toggle() };
};

appModule.component('minimalizaSidebar', {
	controller: ['Sidebar', MinimalizaSidebarController],
	template: '<a class="navbar-minimalize minimalize" href="" ng-click="$ctrl.toggleSidebar()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>'
});
