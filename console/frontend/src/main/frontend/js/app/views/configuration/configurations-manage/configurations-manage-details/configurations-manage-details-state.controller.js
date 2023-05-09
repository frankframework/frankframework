export default function($state) {
	if ($state.params && $state.params.name && $state.params.name != "")
		$state.$current.data.breadcrumbs = "Configurations > Manage > " + $state.params.name;
	else
		$state.go("pages.manage_configurations");
}
