import { Component, input } from '@angular/core';
import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { KeyValueProperty } from '../environment-variables.component';
import { LiteralPipe } from '../../../pipes/literal.pipe';

@Component({
  selector: 'app-environment-variables-table',
  imports: [OrderByPipe, LiteralPipe],
  templateUrl: './environment-variables-table.component.html',
  styleUrl: './environment-variables-table.component.scss',
})
export class EnvironmentVariablesTableComponent {
  public readonly name = input<string>('');
  public readonly properties = input<KeyValueProperty[]>([]);
  public readonly searchFilter = input<string>('');
}
