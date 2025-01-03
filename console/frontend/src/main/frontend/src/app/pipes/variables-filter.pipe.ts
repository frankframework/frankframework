import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'variablesFilter',
  standalone: false,
})
export class VariablesFilterPipe implements PipeTransform {
  transform<T>(variables: T[], filterText: string): T[] {
    const returnArray = [];

    filterText = filterText.toLowerCase();

    for (const index in variables) {
      const variable = variables[index];
      if (JSON.stringify(variable).toLowerCase().includes(filterText)) {
        returnArray.push(variable);
      }
    }

    return returnArray;
  }
}
