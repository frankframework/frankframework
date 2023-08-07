import { appModule } from "../app.module";

appModule.service('Alert', ['$timeout', 'Session', function ($timeout, Session) {
	this.add = function (level, message, non_repeditive) {
		if (non_repeditive === true)
			if (this.checkIfExists(message))
				return;

		var type;
		switch (level) {
			case "info":
			case 1:
				type = "fa fa-info";
				break;
			case "warning":
			case 2:
				type = "fa fa-warning";
				break;
			case "severe":
			case 3:
				type = "fa fa-times";
				break;
			default:
				type = "fa fa-info";
				break;
		}
		var list = this.get(true);
		var obj = {
			type: type,
			message: message,
			time: new Date().getTime()
		};
		list.unshift(obj);
		obj.id = list.length;
		Session.set("Alert", list);
		//sessionStorage.setItem("Alert", JSON.stringify(list));
	};
	this.get = function (preserveList) {
		//var list = JSON.parse(sessionStorage.getItem("Alert"));
		var list = Session.get("Alert");
		if (preserveList == undefined) Session.set("Alert", []); //sessionStorage.setItem("Alert", JSON.stringify([])); //Clear after retreival
		return (list != null) ? list : [];
	};
	this.getCount = function () {
		return this.get(true).length || 0;
	};
	this.checkIfExists = function (message) {
		var list = this.get(true);
		if (list.length > 0) {
			for (var i = 0; i < list.length; i++) {
				if (list[i].message == message) {
					return true;
				}
			}
		}
		return false;
	};
}]);
