import { appModule } from "../../app.module";

const HamburgerController = function (Sidebar) {
	const ctrl = this;
	ctrl.toggleSidebar = function () { Sidebar.toggle() };
}

appModule.component('hamburger', {
	controller: ['Sidebar', HamburgerController],
	template: '<a class="hamburger btn btn-primary " href="" ng-click="$ctrl.toggleSidebar()"><i class="fa fa-bars"></i></a>',
});
