import { appModule } from "../../app.module";

/** @param {JQuery} $element */
const InputFileUploadController = function ($element) {
	const ctrl = this;

	ctrl.$postLink = function(){
		$element.find("input").bind("change", function () {
			ctrl.handleFile(this.files);
		});
	}

	ctrl.handleFile = function (files) {
		if (files.length == 0) {
			ctrl.onUpdateFile(null);
			return;
		}
		ctrl.onUpdateFile(files[0]); //Can only parse 1 file!
	}
}

appModule.component('inputFileUpload', {
	bindings: {
		file: '<',
		onUpdateFile: '&', // add binding to retrieve file update function from parent
		accept: '@',
		title: '@'
	},
	controller: ['$element', InputFileUploadController],
	transclude: true,
	template: '<input class="form-control form-file" name="file" type="file" accept="ctrl.accept" title="ctrl.title" />'
});
