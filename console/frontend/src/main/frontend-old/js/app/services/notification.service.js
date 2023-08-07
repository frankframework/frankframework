import Tinycon from 'tinycon';
import { appModule } from "../app.module";

appModule.service('Notification', ['$rootScope', '$timeout', function ($rootScope, $timeout) {
	Tinycon.setOptions({
		background: '#f03d25'
	});
	this.list = [];
	this.count = 0;
	this.add = function (icon, title, msg, fn) {
		var obj = {
			icon: icon,
			title: title,
			message: (msg) ? msg : false,
			fn: (fn) ? fn : false,
			time: new Date().getTime()
		};
		this.list.unshift(obj);
		obj.id = this.list.length;
		this.count++;

		Tinycon.setBubble(this.count);
	};
	this.get = function (id) {
		for (var i = 0; i < this.list.length; i++) {
			var notification = this.list[i];
			if (notification.id == id) {
				if (notification.fn) {
					$timeout(function () {
						notification.fn.apply(this, notification);
					}, 50);
				}
				return notification;
			}
		}

		return false;
	};
	this.resetCount = function () {
		Tinycon.setBubble(0);
		this.count = 0;
	};
	this.getCount = function () {
		return this.count;
	};
	this.getLatest = function (amount) {
		if (amount < 1) amount = 1;
		return this.list.slice(0, amount);
	};
}]);
