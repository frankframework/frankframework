import { appModule } from "../app.module";

appModule.service('Debug', function () {
	var level = 0; //ERROR
	var levelEnums = ["ERROR", "WARN", "INFO", "DEBUG"];
	var inGroup = false;
	this.getLevel = function () {
		return level;
	};
	this.setLevel = function (l) {
		l = Math.min(3, Math.max(0, l));
		if (l == level) return;
		console.info(this.head() + " Setting LOG level to [" + levelEnums[l] + "]");
		level = l;
	};
	this.head = function (level) {
		var d = new Date();
		var date = ('0' + d.getUTCDate()).slice(-2) + "-" + ('0' + d.getUTCMonth()).slice(-2) + "-" + d.getUTCFullYear();
		date += " " + ('0' + d.getSeconds()).slice(-2) + ":" + ('0' + d.getMinutes()).slice(-2) + ":" + ('0' + d.getHours()).slice(-2);
		if (level != undefined)
			return date + " [" + levelEnums[level] + "] -";
		else
			return date + " -";
	};
	this.log = function () {
		if (level < 3) return;
		var args = arguments || [];
		var func = window.console.log;
		if (!inGroup)
			Array.prototype.unshift.call(args, this.head(3));
		try {
			func.apply(window.console, args);
		} catch (e) {
			for (var a in args)
				console.log(args[a]);
		};
	};
	this.group = function () {
		var args = arguments || [];
		var title = Array.prototype.shift.call(args);
		inGroup = true;
		window.console.group(this.head() + " " + title);

		if (args.length > 0) { //Loop through args and close group after...
			for (var a in args)
				console.log(args[a]);
			this.groupEnd();
		}
	};
	this.groupEnd = function () {
		inGroup = false;
		window.console.groupEnd();
	};
	this.info = function () {
		if (level < 2) return;
		var args = arguments || [];
		var func = window.console.info;
		if (!inGroup)
			Array.prototype.unshift.call(args, this.head(2));
		try {
			func.apply(window.console, args);
		} catch (e) {
			for (var a in args)
				console.info(args[a]);
		};
	};
	this.warn = function (a) {
		if (level < 1) return;
		var args = arguments || [];
		var func = window.console.warn;
		if (!inGroup)
			Array.prototype.unshift.call(args, this.head(1));
		try {
			func.apply(window.console, args);
		} catch (e) {
			for (var a in args)
				console.warn(args[a]);
		};
	};
	this.error = function (a) {
		var args = arguments || [];
		var func = window.console.error;
		if (!inGroup)
			Array.prototype.unshift.call(args, this.head(0));
		try {
			func.apply(window.console, args);
		} catch (e) {
			for (var a in args)
				console.error(args[a]);
		};
	};
});
