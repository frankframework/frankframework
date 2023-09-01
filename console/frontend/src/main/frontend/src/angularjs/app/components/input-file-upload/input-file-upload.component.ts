import { appModule } from "../../app.module";

export class InputFileUploadController {
	constructor(private $element: JQuery) { }

	$postLink() {
		const ctrl = this;
		this.$element.find("input").bind("change", function (this: HTMLInputElement) {
			ctrl.handleFile(this.files);
		});
	}

	handleFile(files: FileList | null) {
		if (!files || files.length == 0) {
			// @ts-expect-error binding
			this.onUpdateFile({ file: null });
			return;
		}
		// @ts-expect-error binding
		this.onUpdateFile({ file: files[0] }); //Can only parse 1 file!
	}
}

appModule.component('inputFileUpload', {
	bindings: {
		file: '<',
		onUpdateFile: '&',
		accept: '@',
		title: '@'
	},
	controller: ['$element', InputFileUploadController],
	transclude: true,
	template: '<input class="form-control form-file" name="file" type="file" accept="{{$ctrl.accept}}" title="{{$ctrl.title}}" />'
});
