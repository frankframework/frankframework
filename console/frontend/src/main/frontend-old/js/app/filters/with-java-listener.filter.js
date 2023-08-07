import { appModule } from "../app.module";

appModule.filter('withJavaListener', function () {
	return function (adapters) {
		if (!adapters) return;
		let schedulerEligibleAdapters = {};
		for (const adapter in adapters) {
			let receivers = adapters[adapter].receivers;
			for (const r in receivers) {
				let receiver = receivers[r];
				if (receiver.listener.class.startsWith('JavaListener')) {
					schedulerEligibleAdapters[adapter] = adapters[adapter];
				}
			}
		}
		return schedulerEligibleAdapters;
	};
});
