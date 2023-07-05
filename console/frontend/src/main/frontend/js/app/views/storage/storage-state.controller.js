export default function ($state) {
	$state.current.data.pageTitle = $state.params.processState + " List";
	$state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List";
}
