export default function($state) {
	$state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List > View Message " + $state.params.messageId;
}
