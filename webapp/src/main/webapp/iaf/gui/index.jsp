<%@ page import="nl.nn.adapterframework.util.AppConstants"%>
<%
// Calculate an unique hash (per framework version) to disable caching
String time = ""+System.currentTimeMillis();
String version = AppConstants.getInstance().getString("application.version", "");
if(version.isEmpty()) {
	version = time;
} else if(version.contains("SNAPSHOT")) {
	version = version + "-" + time; //Append time to disable cache
}
%>
<!DOCTYPE html>
<html ng-app="iaf.beheerconsole">

<head>
	<meta charset="utf-8">
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="pragma" content="no-cache">
	<base href="">

	<title page-title></title>

	<link href="css/bootstrap.min.css" rel="stylesheet">
	<link href="font-awesome/css/font-awesome.css" rel="stylesheet">
	<link href="css/animate.css" rel="stylesheet">

	<link href="css/plugins/sweetalert/sweetalert.css" rel="stylesheet">
	<link href="css/plugins/iCheck/custom.css" rel="stylesheet">
	<link href="css/plugins/ladda/ladda-themeless.min.v1.0.5.css" rel="stylesheet">
	<link href="css/plugins/toastr/toastr.min.css" rel="stylesheet">
	<link id="loadBefore" href="css/style.css?v=<%=version%>" rel="stylesheet">
	<link rel="shortcut icon" href="favicon.ico">
</head>

<body ng-class="{'gray-bg': ($state.current.name.indexOf('pages.')===1) }" id="page-top">

<div class="loading" style="z-index: 999; position: fixed; top: 0; bottom: 0; left: 0; background: #f8f8f8; right: 0; padding-top: 100px;">
	<div class="middle-box text-center loginscreen  animated fadeInDown">
		<div>
			<div>
				<h2>Loading</h2>
			</div>
			<div class="sk-spinner sk-spinner-wave">
				<div class="sk-rect1"></div>
				<div class="sk-rect2"></div>
				<div class="sk-rect3"></div>
				<div class="sk-rect4"></div>
				<div class="sk-rect5"></div>
			</div>
		</div>
	</div>
</div>

<toaster-container toaster-options="{'time-out':5500, 'close-button': true}"></toaster-container>

<div ui-view style="display:none;" class="main"></div>

<div general-data-protection-regulation></div>


<script src="js/jquery/jquery-2.1.1.min.js"></script>
<script src="js/plugins/jquery-ui/jquery-ui.js"></script>
<script src="js/bootstrap/bootstrap.min.js"></script>

<script src="js/plugins/metisMenu/jquery.metisMenu.js"></script>
<script src="js/plugins/slimscroll/jquery.slimscroll.min.js"></script>
<script data-pace-options='{ "ajax": false }' src="js/plugins/pace/pace.min.js"></script>
<script async src="https://www.googletagmanager.com/gtag/js?id=UA-111373008-1"></script>

<script src="js/main.js"></script>

<!-- Main Angular scripts-->
<script src="js/angular/angular.min.js"></script>
<script src="js/angular/angular-sanitize.js"></script>
<script src="js/angular/angular-cookies.js"></script>
<script src="js/plugins/oclazyload/dist/ocLazyLoad.min.v1.0.10.js"></script>
<script src="js/ui-router/angular-ui-router.min.js"></script>
<script src="js/bootstrap/ui-bootstrap-tpls-1.1.2.min.js"></script>
<script src="js/plugins/angular-idle/angular-idle.js"></script>
<script src="js/plugins/sweetalert/sweetalert.min.js"></script>
<script src="js/plugins/tinycon/tinycon.min.js"></script>
<script src="js/plugins/iCheck/icheck.min.js"></script>
<script src="js/plugins/ladda/spin.min.v1.0.5.js"></script>
<script src="js/plugins/ladda/ladda.min.v1.0.5.js"></script>
<script src="js/plugins/ladda/angular-ladda.min.v0.4.3.js"></script>
<script src="js/plugins/toastr/toastr.min.js"></script>

<script type="text/javascript">var ff_version="<%=version%>"</script>

<script src="js/app.js?v=<%=version%>"></script>
<script src="js/config.js?v=<%=version%>"></script>
<script src="js/services.js?v=<%=version%>"></script>
<script src="js/directives.js?v=<%=version%>"></script>
<script src="js/controllers.js?v=<%=version%>"></script>

</body>
</html>