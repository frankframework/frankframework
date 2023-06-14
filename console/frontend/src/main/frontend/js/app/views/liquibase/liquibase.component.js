import { appModule } from "../../app.module";

const LiquibaseController = function ($scope, Api, Misc) {
    const ctrl = this;

    ctrl.form = {};
    ctrl.file = null;
    ctrl.generateSql = false;
    
    ctrl.$onInit = function () {
        let findFirstAvailabeConfiguration = function () {
            for (let i in ctrl.configurations) {
                let configuration = ctrl.configurations[i];
    
                if (configuration.jdbcMigrator) {
                    ctrl.form.configuration = configuration.name;
                    break;
                };
            };
        };

        findFirstAvailabeConfiguration();
        $scope.$on('configurations', findFirstAvailabeConfiguration);
    };

    ctrl.download = function () {
        window.open(Misc.getServerPath() + "iaf/api/jdbc/liquibase/");
    };

    ctrl.submit = function (formData) {
        if (!formData) formData = {};
        var fd = new FormData();
        ctrl.generateSql = true;

        if (ctrl.file != null) {
            fd.append("file", ctrl.file);
        };

        fd.append("configuration", formData.configuration);

        Api.Post("jdbc/liquibase", fd, function (returnData) {
            ctrl.error = "";
            ctrl.generateSql = false;
            $.extend(ctrl, returnData);
        }, function (errorData, status, errorMsg) {
            ctrl.generateSql = false;
            var error = (errorData) ? errorData.error : errorMsg;
            ctrl.error = error;
            ctrl.result = "";
        }, false);
    };
};

appModule.component('liquibase', {
    controller: ['$scope', 'Api', 'Misc', LiquibaseController],
    templateUrl: 'js/app/views/liquibase/liquibase.component.html'
});
