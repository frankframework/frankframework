import { HttpClient } from '@angular/common/http';
import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { DataTableDirective } from 'angular-datatables';
import { ADTSettings } from 'angular-datatables/src/models/settings';
import { AppService } from 'src/app/app.service';
import { copyToClipboard } from 'src/app/utils';

type Connections = {
  data: {
    domain: string;
    destination: string;
    adapterName: string;
    componentName: string;
    direction: string;
  }[];
};

@Component({
  selector: 'app-connections',
  templateUrl: './connections.component.html',
  styleUrls: ['./connections.component.scss'],
})
export class ConnectionsComponent implements OnInit, AfterViewInit {
  @ViewChild(DataTableDirective, { static: false }) datatableElement!: DataTableDirective;

  protected dtOptions: ADTSettings = {};

  private minimalTruncateLength = 100;

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.dtOptions = {
      processing: true,
      lengthMenu: [50, 100, 250, 500],
      ordering: false,
      columns: [
        { data: 'adapterName' },
        { data: 'componentName' },
        { data: 'domain' },
        { data: 'destination' },
        { data: 'direction' },
      ],
      ajax: (data, callback): void => {
        this.http.get<Connections>(`${this.appService.absoluteApiPath}connections`).subscribe((response) => {
          callback({
            ...response,
            draw: (data as Record<string, unknown>)['draw'],
            recordsTotal: response.data.length,
            recordsFiltered: response.data.length,
          });
        });
      },
      initComplete: (): undefined => {
        this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
          dtInstance.columns([2, 4]).every(function () {
            // eslint-disable-next-line unicorn/no-this-assignment, @typescript-eslint/no-this-alias
            const column = this;

            const select = $('<select><option value=""></option></select>')
              .appendTo($(column.header()))
              .on('change', function (event) {
                const input = event.target as HTMLInputElement;
                if (column.search() !== input['value']) {
                  column.search(input['value']).draw();
                }
              });

            column
              .data()
              .unique()
              .sort()
              .each(function (d) {
                select.append(`<option value="${d}">${d}</option>`);
              });
          });
        });
      },
      columnDefs: [
        {
          targets: [0, 1, 3],
          render: (data, type): unknown => {
            if (type === 'display' && typeof data == 'string' && data.length > this.minimalTruncateLength) {
              const title = data.replaceAll('"', '&quot;');
              const leftTrancate = data.slice(0, 15);
              const rightTrancate = data.slice(-15);
              data = `<span title="${title}">${leftTrancate}&#8230;${rightTrancate}</span>`;
            }
            return data;
          },
        },
      ],
    };
  }

  ngAfterViewInit(): void {
    this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
      dtInstance.columns([0, 1, 3]).every(function () {
        // eslint-disable-next-line unicorn/no-this-assignment, @typescript-eslint/no-this-alias
        const column = this;

        column.nodes().on('click', (event) => {
          const target = event.target as HTMLElement;
          if (target.title != '') {
            copyToClipboard(target.title);
          }
        });

        $('input', this.header()).on('keyup change', function (event) {
          const input = event.target as HTMLInputElement;
          if (column.search() !== input['value']) {
            column.search(input['value']).draw();
          }
        });
      });
    });
  }
}
