import { Component, Input } from '@angular/core';
import { CdkTableModule, DataSource } from '@angular/cdk/table';
import { CommonModule } from '@angular/common';

export type DataTableColumn<T> = {
  name: string;
  displayName: string;
  property: keyof T;
  html?: boolean;
};

@Component({
  selector: 'app-datatable',
  standalone: true,
  imports: [CommonModule, CdkTableModule],
  templateUrl: './datatable.component.html',
  styleUrl: './datatable.component.scss',
})
export class DatatableComponent<T> {
  @Input({ required: true }) public datasource!: DataSource<T>;
  @Input({ required: true }) public displayColumns: DataTableColumn<T>[] = [];

  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected displayedColumns: string[] = this.displayColumns.map((column) => column.name);
}
