import { AppService, Configuration } from "../../app.service";
import { ApiService } from "../../services/api.service";
import { MiscService } from "../../services/misc.service";

class LiquibaseController {
    form: Record<string, any> = {};
    file = null;
    generateSql = false;
    error = "";
    result = "";
    configurations: Configuration[] = [];

    constructor(
        private Api: ApiService,
        private Misc: MiscService,
        private appService: AppService,
    ) { };

    $onInit() {
        let findFirstAvailabeConfiguration = () => {
            this.configurations = this.appService.configurations;

            for (let i in this.configurations) {
                let configuration = this.configurations[i];

                if (configuration.jdbcMigrator) {
                    this.form['configuration'] = configuration.name;
                    break;
                };
            };
        };

        this.appService.configurations$.subscribe(() => findFirstAvailabeConfiguration());
        findFirstAvailabeConfiguration();
    };

    download() {
        window.open(this.Misc.getServerPath() + "iaf/api/jdbc/liquibase/");
    };

    submit(formData: any) {
        if (!formData) formData = {};
        var fd = new FormData();
        this.generateSql = true;

        if (this.file != null) {
            fd.append("file", this.file);
        };

        fd.append("configuration", formData.configuration);

        this.Api.Post("jdbc/liquibase", fd, (returnData) => {
            this.error = "";
            this.generateSql = false;
            $.extend(this, returnData);
        }, (errorData, status, errorMsg) => {
            this.generateSql = false;
            var error = (errorData) ? errorData.error : errorMsg;
            this.error = error;
            this.result = "";
        }, false);
    };
};