import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'variablesFilter' })
export class VariablesFilterPipe implements PipeTransform {
  transform<T>(variables: T[], filterText: string): T[] {
    var returnArray = new Array();

    filterText = filterText.toLowerCase();

    for (const index in variables) {
      var variable = variables[index];
      if (JSON.stringify(variable).toLowerCase().includes(filterText)) {
        returnArray.push(variable);
      }
    }

    return returnArray;
  }
}
