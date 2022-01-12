if(location.hostname != "localhost") {
	window.console.log("%cThis is a browser feature intended for developers. Do not paste any code here given to you by someone else. It may compromise your account or have other negative side effects.","font-weight: bold; font-size: 14px;");
}

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

	window.addEventListener("keydown", function(e) {
		if(e.which == 70 && (e.ctrlKey || e.metaKey)) {
			var searchbar = document.getElementById("searchbar");
			if(searchbar) {
				e.preventDefault();
				searchbar.getElementsByTagName("input")[0].focus();
			}
		}
	});
});

//Foist: To force upon or impose fraudulently or unjustifiably
function foist(callback) {
	angular.element(document.body).scope().foist(callback);
}
//Changes the log level to; 0 - error, 1 - warn, 2 - info, 3 - debug
function setLogLevel(level) {
	angular.element(document.body).scope().setLogLevel(level);
}
//Detect if using any (older) version of Internet Explorer
if(navigator.userAgent.indexOf('MSIE') !== -1 || navigator.appVersion.indexOf('Trident/') > -1) {
	$("body").prepend("<h2 style='text-align: center; color: #fdc300;'><strong>Internet Explorer 11 and older do not support XHR requests, the Frank!Console might not load correctly!</strong><br/>Please open this website in MS Edge, Mozilla Firefox or Google Chrome.</h2>");
}

// Automatically minimalize menu when screen is less than 768px
$(function() {
	$(window).on("load resize", function() {
		if ($(document).width() < 769) {
			$("body").addClass("body-small");
		} else {
			$("body").removeClass("body-small");
		}
	});

	$(window).on("scroll", function() {
		var scroll2top = $(".scroll-to-top").stop(true);
		if($(this).scrollTop() > 100) {
			if(parseInt(scroll2top.css("opacity")) === 0) {
				scroll2top.animate({"opacity": 1, "z-index": 10000}, 50, "linear");
			}
		} else {
			scroll2top.animate({"opacity": 0, "z-index": -1}, 50, "linear");
		}
	});
});
