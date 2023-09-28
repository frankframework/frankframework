import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'variablesFilter' })
export class VariablesFilterPipe implements PipeTransform {
  transform<T>(variables: T[], filterText: string): T[] {

    var returnArray = new Array();

    filterText = filterText.toLowerCase();

    for (const i in variables) {
      var variable = variables[i];
      if (JSON.stringify(variable).toLowerCase().indexOf(filterText) > -1) {
        returnArray.push(variable);
      };
    };

    return returnArray;
  };
};
