$(document).ready(function () {
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
