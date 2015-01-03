$( document ).ready(function() {
	/* start clock */
	runClock();
	
	 var pgurl = window.location.href.substr(window.location.href.lastIndexOf("/")+1);
		 $(".navbar-collapse li a, .nav li").each(function(){
			 $(this).removeClass("active"); 
		 if($(this).attr("href") == pgurl || $(this).attr("href") == '' )
			 $(this).addClass("active");
	});
		 
	
});

var serverDate = new Date();
serverDate.setFullYear(sd.substring(0,4));
serverDate.setMonth(sd.substring(5,7)-1);
serverDate.setDate(sd.substring(8,10));
serverDate.setHours(sd.substring(11,13));
serverDate.setMinutes(sd.substring(14,16));
serverDate.setSeconds(sd.substring(17,19));
serverDate.setMilliseconds(sd.substring(20));
var diff = 0;

function setDiffTime() {
	diffTime = clientDate.getTime() - serverDate.getTime();
	diff = 1;
}

function runClock() {
	clientDate = new Date();
	if (diff==0) {
		setDiffTime(clientDate);
	}
	serverDate.setTime(clientDate.getTime() - diffTime);
	var year = serverDate.getFullYear();
	var month = serverDate.getMonth()+1;
	if (month <= 9) {
		month = "0" + month;
	}
	var day = serverDate.getDate();
	if (day <= 9) {
		day = "0" + day;
	}
	var hours = serverDate.getHours();
	if (hours <= 9) {
		hours = "0" + hours;
	}
	var minutes = serverDate.getMinutes();
	if (minutes <= 9) {
		minutes = "0" + minutes;
	}
	var seconds = serverDate.getSeconds();
	if (seconds <= 9) {
		seconds = "0" + seconds;
	}
	var dateString = "current date and time: " + year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
	document.getElementById("clock").innerHTML = dateString;
	setTimeout("runClock()",1000);
}
