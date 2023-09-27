import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatStatistics'
})
export class FormatStatisticsPipe implements PipeTransform {

  transform<T extends Record<string, any>>(input: T, format: Record<string, any>): T {
    let formatted: T = {} as T;
    for (const key in format) {
      let value = input[key];
      if (!value && value !== 0) { // if no value, return a dash
        value = "-";
      }
      if ((key.endsWith("ms") || key.endsWith("B")) && value != "-") {
        value += "%";
      }
      formatted[key as keyof T] = value;
    }
    // Uncomment if really needed, but figure out how angular2+ handles this first
    // formatted["$$hashKey"] = input["$$hashKey"]; //Copy the hashKey over so Angular doesn't trigger another digest cycle
    return formatted;
  }

}
