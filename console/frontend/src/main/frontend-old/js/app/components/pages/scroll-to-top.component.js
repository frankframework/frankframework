import { appModule } from "../../app.module";

const ScrollToTopController = function () {
	const ctrl = this;
	ctrl.scrollTop = function () {
		$(window).scrollTop(0);
	};
};

appModule.component('scrollToTop', {
	controller: ScrollToTopController,
	template: '<div class="scroll-to-top"><a title="Scroll to top" ng-click="$ctrl.scrollTop()"><i class="fa fa-arrow-up"></i> <span class="nav-label">Scroll To Top</span></a></div>'
});
