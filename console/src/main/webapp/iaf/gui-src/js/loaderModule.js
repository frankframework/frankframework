function lazySystem ($ocLazyLoad) {
	function load (src, key) {
		return import(src).then(function (loadedFile) {
			return $ocLazyLoad.load(loadedFile[key || 'default']);
		});
	}

	this.load = load;
}

const loaderModule = angular
		.module('loaderModule', ['oc.lazyLoad'])
		.config(['$ocLazyLoadProvider'], function config($ocLazyLoadProvider) {
			$ocLazyLoadProvider.config({
				debug: true
			})
		})
		.service('lazyLoader', lazySystem);

export { loaderModule };
