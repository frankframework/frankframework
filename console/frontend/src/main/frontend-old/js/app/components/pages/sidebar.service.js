import { appModule } from "../../app.module";

appModule.service('Sidebar', function () {
	this.toggle = function () {
		$("body").toggleClass("mini-navbar");
		if (!$('body').hasClass('mini-navbar') || $('body').hasClass('body-small')) {
			// Hide menu in order to smoothly turn on when maximize menu
			$('#side-menu').hide();
			// For smoothly turn on menu
			setTimeout(
				function () {
					$('#side-menu').fadeIn(400);
				}, 200);
		} else if ($('body').hasClass('fixed-sidebar')) {
			$('#side-menu').hide();
			setTimeout(
				function () {
					$('#side-menu').fadeIn(400);
				}, 100);
		} else {
			// Remove all inline style from jquery fadeIn function to reset menu state
			$('#side-menu').removeAttr('style');
		}
	}
});
