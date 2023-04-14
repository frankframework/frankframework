<%@ page import="nl.nn.adapterframework.util.AppConstants" %>
<% // Calculate an unique hash (per framework version) to disable caching
  String time="" +System.currentTimeMillis();
  String version=AppConstants.getInstance().getString("application.version", "" ); if(version.isEmpty()) {
  version=time; } else if(version.contains("SNAPSHOT")) { version=version + "-" + time; //Append time to disable cache
} %>
<!DOCTYPE html>
<html ng-app="iaf.beheerconsole">

<head>
  <meta charset="utf-8">
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta http-equiv="cache-control" content="no-cache">
  <meta http-equiv="pragma" content="no-cache">
  <base href="">

  <title page-title></title>

  <link rel="shortcut icon" href="images/favicon.ico">
</head>

<body ng-class="{'gray-bg': ($state.current.name.indexOf('pages.')===1) }" id="page-top">

  <div class="loading"
    style="z-index: 999; position: fixed; top: 0; bottom: 0; left: 0; background: #f8f8f8; right: 0; padding-top: 100px;">
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

  <toaster-container
    toaster-options="{'time-out':5500, 'close-button': true, 'prevent-duplicates':true}"></toaster-container>

  <div ui-view style="display:none;" class="main"></div>

  <script type="text/javascript" nonce="ffVersion">var ff_version = "<%=version%>"</script>
  <script src="js/bundle.js?v=<%=version%>"></script>

</body>

</html>
