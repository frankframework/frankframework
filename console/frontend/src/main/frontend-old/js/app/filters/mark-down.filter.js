import { appModule } from "../app.module";
import MarkdownIt from 'markdown-it';

appModule.filter('markDown', function () {
	return function (input) {
		if (!input) return;
		// input = input.replace(/(?:\r\n|\r|\n)/g, '<br />');
		// input = input.replace(/\[(.*?)\]\((.+?)\)/g, '<a target="_blank" href="$2" alt="$1">$1</a>');
		const md = MarkdownIt({
			html: true,
			linkify: true,
			typographer: false
		});
		return md.render(input);;
	};
});
