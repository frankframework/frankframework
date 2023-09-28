import { Component, OnInit } from '@angular/core';
import { AppService, Configuration, File } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';

interface Form {
    configuration: string
}

@Component({
    selector: 'app-liquibase',
    templateUrl: './liquibase.component.html',
    styleUrls: ['./liquibase.component.scss']
})
export class LiquibaseComponent implements OnInit {
    form: Form = {
        configuration: ""
    };
    file: File | null = {
        name: ""
    };
    generateSql: boolean = false;
    error: string = "";
    result: string = "";
    configurations: Configuration[] = [];
    filteredConfigurations: Configuration[] = [];

    constructor(
        private apiService: ApiService,
        private miscService: MiscService,
        private appService: AppService,
    ) { };

    ngOnInit(): void {
        let findFirstAvailabeConfiguration = () => {
            this.configurations = this.appService.configurations;
            this.filteredConfigurations = this.configurations.filter((item) => item.jdbcMigrator === true);

            for (let i in this.filteredConfigurations) {
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
        window.open(this.miscService.getServerPath() + "iaf/api/jdbc/liquibase/");
    };

    submit(formData: Form) {
        if (!formData) formData = { configuration: "" };
        var fd = new FormData();
        this.generateSql = true;

        if (this.file != null) {
            fd.append("file", this.file as any);
        };

        fd.append("configuration", formData.configuration);

        this.apiService.Post("jdbc/liquibase", fd, (returnData) => {
            this.error = "";
            this.generateSql = false;
            Object.assign(this, returnData);
        }, (errorData, status, errorMsg) => {
            this.generateSql = false;
            var error = (errorData) ? errorData.error : errorMsg;
            this.error = error;
            this.result = "";
        }, false);
    };
}
