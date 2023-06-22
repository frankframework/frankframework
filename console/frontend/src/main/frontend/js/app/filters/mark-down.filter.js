import { appModule } from "../app.module";

appModule.filter('markDown', function () {
	return function (input) {
		if (!input) return;
		input = input.replace(/(?:\r\n|\r|\n)/g, '<br />');
		input = input.replace(/\[(.*?)\]\((.+?)\)/g, '<a target="_blank" href="$2" alt="$1">$1</a>');
		return input;
	};
});
