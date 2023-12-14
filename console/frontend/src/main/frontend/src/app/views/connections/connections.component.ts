import { HttpClient } from '@angular/common/http';
import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { DataTableDirective } from 'angular-datatables';
import { ADTSettings } from 'angular-datatables/src/models/settings';
import { AppService } from 'src/app/app.service';

type Connections = {
  data: {
    domain: string,
    destination: string,
    adapterName: string,
    componentName: string,
    direction: string
  }[]
}

@Component({
  selector: 'app-connections',
  templateUrl: './connections.component.html',
  styleUrls: ['./connections.component.scss']
})
export class ConnectionsComponent implements OnInit, AfterViewInit {
  @ViewChild(DataTableDirective, { static: false })
  datatableElement!: DataTableDirective;
  dtOptions: ADTSettings = {};

  constructor(
    private http: HttpClient,
    private appService: AppService
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
      ajax: (data: Record<any, any>, callback, settings) => {
        this.http.get<Connections>(this.appService.absoluteApiPath + "connections").subscribe((response) => {
          callback({
            ...response,
            draw: data['draw'],
            recordsTotal: response.data.length,
            recordsFiltered: response.data.length
           });
        });
      },
      initComplete: () => {
        this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
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
    this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
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
