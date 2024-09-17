import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { DataTableColumn, DataTableDataSource } from '../../components/datatable/datatable.component';

type Connections = {
  data: {
    domain: string;
    destination: string;
    adapterName: string;
    componentName: string;
    direction: string;
  }[];
};

type ConnectionsData = Connections['data'][number];

@Component({
  selector: 'app-connections',
  templateUrl: './connections.component.html',
  styleUrls: ['./connections.component.scss'],
})
export class ConnectionsComponent implements OnInit {
  protected datasource: DataTableDataSource<ConnectionsData> = new DataTableDataSource();
  protected displayedColumns: DataTableColumn<ConnectionsData>[] = [
    { name: 'adapterName', displayName: 'Adapter Name', property: 'adapterName' },
    { name: 'componentName', displayName: 'Listener/Sender Name', property: 'componentName' },
    { name: 'domain', displayName: 'Domain', property: 'domain' },
    { name: 'destination', displayName: 'Destination', property: 'destination' },
    { name: 'direction', displayName: 'Direction', property: 'direction' },
  ];

  protected minimalTruncateLength = 100;

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.http.get<Connections>(`${this.appService.absoluteApiPath}connections`).subscribe((response) => {
      this.datasource.data = response.data;
    });
  }
}
