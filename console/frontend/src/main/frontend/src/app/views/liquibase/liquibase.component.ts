import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppService, Configuration } from 'src/app/app.service';
import { JdbcService } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';

interface Form {
  configuration: string
}

@Component({
  selector: 'app-liquibase',
  templateUrl: './liquibase.component.html',
  styleUrls: ['./liquibase.component.scss']
})
export class LiquibaseComponent implements OnInit, OnDestroy {
  form: Form = {
    configuration: ""
  };
  file: File | null = null;
  generateSql: boolean = false;
  error: string = "";
  result: string = "";
  configurations: Configuration[] = [];
  filteredConfigurations: Configuration[] = [];

  private _subscriptions = new Subscription();

  constructor(
    private appService: AppService,
    private jdbcService: JdbcService
  ) { };

  ngOnInit(): void {
    const findFirstAvailabeConfiguration = () => {
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

    const configurationsSubscription = this.appService.configurations$.subscribe(() => findFirstAvailabeConfiguration());
    this._subscriptions.add(configurationsSubscription);
    findFirstAvailabeConfiguration();
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  download() {
    window.open(this.appService.getServerPath() + "iaf/api/jdbc/liquibase/");
  };

  submit(formData: Form) {
    if (!formData) formData = { configuration: "" };
    var fd = new FormData();
    this.generateSql = true;

    if (this.file != null) {
      fd.append("file", this.file as any);
    };

    fd.append("configuration", formData.configuration);

    this.jdbcService.postJdbcLiquibase(fd).subscribe({ next: returnData => {
      this.error = "";
      this.generateSql = false;
      this.result = returnData.result;
    }, error: (errorData: HttpErrorResponse) => {
      this.generateSql = false;
      const error = (errorData && errorData.error) ? errorData.error : "An error occured!";
      this.error = typeof error === 'object' ? error.error : error;
      this.result = "";
    }}); // TODO no intercept
  };
}
