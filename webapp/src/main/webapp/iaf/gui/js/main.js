if(location.hostname != "localhost") {
	window.console.log("%cThis is a browser feature intended for developers. Do not paste any code here given to you by someone else. It may compromise your account or have other negative side effects.","font-weight: bold; font-size: 14px;");
}

window.dataLayer = window.dataLayer || [];
function gtag(){dataLayer.push(arguments);}

console.time("startup");
console.time("documentReady");

/* Main.js */
$(document).ready(function () {
	console.timeEnd("documentReady");
	console.log("Launching GUI!");

	// Full height of sidebar
	function fix_height_func() {
		var navbarHeight = $('nav.navbar-default').height();
		var wrapperHeight = $('#page-wrapper').height();

		if(navbarHeight <= wrapperHeight && $(window).height() > navbarHeight){
			$('#page-wrapper').css("min-height", $(window).height()  + "px");
		} else {
			$('#page-wrapper').css("min-height", navbarHeight + "px");
		}
		
	}

	$(window).on("resize scroll", function() {
		if(!$("body").hasClass('body-small')) {
			fix_height();
		}
	});
	$(window).on("load", function() {
		if(!$("body").hasClass('body-small')) {
			fix_height(500);
		}
	});
	
	function fix_height(time) {
		if(!time) time = 50;
		setTimeout(function(){
			fix_height_func();
		}, time);
	}
});

//Foist: To force upon or impose fraudulently or unjustifiably
function foist(callback) {
	angular.element(document.body).scope().foist(callback);
}

// Automatically minimalize menu when screen is less than 768px
$(function() {
	$(window).on("load resize", function() {
		if ($(document).width() < 769) {
			$('body').addClass('body-small');
		} else {
			$('body').removeClass('body-small');
		}
	});
});
