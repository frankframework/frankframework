import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { DataTableDirective } from 'angular-datatables';

@Component({
  selector: 'app-connections',
  templateUrl: './connections.component.html',
  styleUrls: ['./connections.component.scss']
})
export class ConnectionsComponent implements OnInit, AfterViewInit {
  @ViewChild(DataTableDirective, { static: false })
  datatableElement: DataTableDirective | undefined;
  dtOptions: DataTables.Settings = {};

  constructor(
    private apiService: ApiService
  ) { };

  ngOnInit(): void {
    this.dtOptions = {
      processing: true,
      lengthMenu: [50, 100, 250, 500],
      ordering: false,
      columns: [
        { "data": "adapterName" },
        { "data": "componentName" },
        { "data": "domain" },
        { "data": "destination" },
        { "data": "direction" }
      ],
      ajax: (data: any, callback: any, settings: any) => {
        this.apiService.Get("connections", function (response) {
          response.draw = data.draw;
          response.recordsTotal = response.data.length;
          response.recordsFiltered = response.data.length;
          callback(response);
        });
      },
      initComplete: () => {
        this.datatableElement!.dtInstance.then((dtInstance: DataTables.Api) => {
          dtInstance.columns([2, 4]).every(function () {
            const column = this;

            var select = $('<select><option value=""></option></select>')
              .appendTo($(column.header()))
              .on('change', function (event) {
                var input = event.target as HTMLInputElement
                if (column.search() !== input['value']) {
                  column
                    .search(input['value'])
                    .draw();
                };
              });

            column.data().unique().sort().each(function (d, j) {
              select.append('<option value="' + d + '">' + d + '</option>')
            });
          });
        });
      }
    }
  };

  ngAfterViewInit(): void {
    this.datatableElement!.dtInstance.then((dtInstance: DataTables.Api) => {
      dtInstance.columns([0, 1, 3]).every(function () {
        const column = this;

        $('input', this.header()).on('keyup change', function (event) {
          var input = event.target as HTMLInputElement
          if (column.search() !== input['value']) {
            column
              .search(input['value'])
              .draw();
          };
        });
      });
    });
  }
};
