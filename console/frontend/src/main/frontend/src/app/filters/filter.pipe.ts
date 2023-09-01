import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'filter'
})
/**@Deprecated Replace with component code */
export class FilterPipe implements PipeTransform {
    transform(array: any[], field: {}): any[] {
        if (!array || !field) {
            return array;
        }
        // TODO filter pipe
        return array;
    }
};