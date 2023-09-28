import { Component, Inject, OnInit } from '@angular/core';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { APPCONSTANTS } from 'src/app/app.module';

interface ColumnName {
    id: number
    name: string
    desc: string
}

interface Form {
    datasource: string
    resultType: string
    table: string
    where: string
    order: string
    numberOfRowsOnly: boolean
    minRow: number
    maxRow: number
}

interface ReturnData {
    query: string
    fielddefinition: Record<string, string>
    result: Record<string, Record<string, string>>
}

@Component({
    selector: 'app-jdbc-browse-tables',
    templateUrl: './jdbc-browse-tables.component.html',
    styleUrls: ['./jdbc-browse-tables.component.scss']
})
export class JdbcBrowseTablesComponent implements OnInit {
    datasources: string[] = [];
    resultTypes: string[] = [];
    error: string = "";
    processingMessage: boolean = false;
    form: Form = {
        datasource: "",
        resultType: "",
        table: "",
        where: "",
        order: "",
        numberOfRowsOnly: false,
        minRow: 0,
        maxRow: 0
    };
    columnNames: ColumnName[] = [];
    result: string[][] = [];
    query: string = "";

    constructor(
        private apiService: ApiService,
        @Inject(APPCONSTANTS) private appConstants: AppConstants,
        private appService: AppService
    ) { };

    ngOnInit(): void {
        this.appService.appConstants$.subscribe(() => {
            this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
        });

        this.apiService.Get("jdbc", (data) => {
            this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
            this.datasources = data.datasources;
        });
    };

    submit(formData: Form) {
        const columnNameArray: string[] = [];
        this.processingMessage = true;

        if (!formData || !formData.table) {
            this.error = "Please specify a datasource and table name!";
            this.processingMessage = false;
            return;
        }

        if (!formData.datasource) formData.datasource = this.datasources[0] || "";
        if (!formData.resultType) formData.resultType = this.resultTypes[0] || "";

        this.apiService.Post("jdbc/browse", JSON.stringify(formData), (returnData: ReturnData) => {
            this.error = "";
            this.query = returnData.query;
            let i = 0;

            for (const x in returnData.fielddefinition) {
                this.columnNames.push({
                    id: i++,
                    name: x,
                    desc: returnData.fielddefinition[x]
                });
                columnNameArray.push(x);
            }

            for (const x in returnData.result) {
                const row = returnData.result[x];
                const orderedRow: string[] = [];

                for (const columnName in row) {
                    const index = columnNameArray.indexOf(columnName);
                    let value = row[columnName];

                    if (index === -1 && columnName.indexOf("LENGTH ") > -1) {
                        value += " (length)";
                        value = row[columnName.replace("LENGTH ", "")];
                    }
                    orderedRow[index] = value;
                }
                this.result.push(orderedRow);
            }

            this.processingMessage = false;
        }, (errorData: { error: string }) => {
            const error = errorData.error ? errorData.error : "";
            this.error = error;
            this.query = "";
            this.processingMessage = false;
        }, false);
    }

    reset() {
        this.query = "";
        this.error = "";
        if (!this.form) return;
        if (this.form["table"]) this.form["table"] = "";
        if (this.form["where"]) this.form["where"] = "";
        if (this.form["order"]) this.form["order"] = "";
        if (this.form["numberOfRowsOnly"]) this.form["numberOfRowsOnly"] = false;
        if (this.form["minRow"]) this.form["minRow"] = 0;
        if (this.form["maxRow"]) this.form["maxRow"] = 0;
    }
}
