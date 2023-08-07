import { appModule } from "../app.module";

appModule.service('Misc', ['appConstants', 'Base64', function (appConstants, Base64) {
	this.getServerPath = function () {
		var absolutePath = appConstants.server;
		if (absolutePath && absolutePath.slice(-1) != "/") absolutePath += "/";
		return absolutePath;
	};
	this.escapeURL = function (uri) {
		return encodeURIComponent(uri);
	}
	this.isMobile = function () {
		return (navigator.userAgent.match(/Android/i)
			|| navigator.userAgent.match(/webOS/i)
			|| navigator.userAgent.match(/iPhone/i)
			|| navigator.userAgent.match(/iPad/i)
			|| navigator.userAgent.match(/iPod/i)
			|| navigator.userAgent.match(/BlackBerry/i)
			|| navigator.userAgent.match(/Windows Phone/i)
		) ? true : false;
	};
	this.getUID = function (serverInfo) {
		let queryObj = {
			"v": serverInfo.framework.version,
			"n": serverInfo.instance.name,
			"s": serverInfo["dtap.stage"],
		};
		let b = Base64.encode(JSON.stringify(queryObj));
		const chunks = [];
		let pos = 0
		while (pos < b.length) {
			chunks.push(b.slice(pos, pos += 5));
		}
		return chunks.reverse().join("");
	};
	this.compare_version = function (v1, v2, operator) {
		// See for more info: http://locutus.io/php/info/version_compare/

		var i, x, compare = 0;
		var vm = {
			'dev': -6,
			'alpha': -5,
			'a': -5,
			'beta': -4,
			'b': -4,
			'RC': -3,
			'rc': -3,
			'#': -2,
			'p': 1,
			'pl': 1
		};

		var _prepVersion = function (v) {
			v = ('' + v).replace(/[_\-+]/g, '.');
			v = v.replace(/([^.\d]+)/g, '.$1.').replace(/\.{2,}/g, '.');
			return (!v.length ? [-8] : v.split('.'));
		};
		var _numVersion = function (v) {
			return !v ? 0 : (isNaN(v) ? vm[v] || -7 : parseInt(v, 10));
		};

		v1 = _prepVersion(v1);
		v2 = _prepVersion(v2);
		x = Math.max(v1.length, v2.length);
		for (i = 0; i < x; i++) {
			if (v1[i] === v2[i]) {
				continue;
			}
			v1[i] = _numVersion(v1[i]);
			v2[i] = _numVersion(v2[i]);
			if (v1[i] < v2[i]) {
				compare = -1;
				break;
			} else if (v1[i] > v2[i]) {
				compare = 1;
				break;
			}
		}
		if (!operator) {
			return compare;
		}

		switch (operator) {
			case '>':
			case 'gt':
				return (compare > 0);
			case '>=':
			case 'ge':
				return (compare >= 0);
			case '<=':
			case 'le':
				return (compare <= 0);
			case '===':
			case '=':
			case 'eq':
				return (compare === 0);
			case '<>':
			case '!==':
			case 'ne':
				return (compare !== 0);
			case '':
			case '<':
			case 'lt':
				return (compare < 0);
			default:
				return null;
		}
	};
}]);
