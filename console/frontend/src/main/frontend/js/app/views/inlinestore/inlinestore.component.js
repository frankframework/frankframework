import { appModule } from "../../app.module";

const InlineStoreController = function (Api) {
    const ctrl = this;

    Api.Get("inlinestores/overview", function (data) {
		ctrl.result = data;
	});
};

appModule.component('inlineStore', {
    controller: ['Api', InlineStoreController],
    templateUrl: 'js/app/views/inlinestore/inlinestore.component.html'
});
