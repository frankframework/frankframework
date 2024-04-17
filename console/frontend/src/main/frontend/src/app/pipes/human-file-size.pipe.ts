import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'humanFileSize',
  standalone: true,
})
export class HumanFileSizePipe implements PipeTransform {
  transform(bytes: number, useSiUnits = false, decimalPlaces = 1): string {
    const threshold = useSiUnits ? 1000 : 1024;

    if (Math.abs(bytes) < threshold) {
      return `${bytes} B`;
    }

    const units = useSiUnits
      ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
      : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    let index = -1;
    const roundingPrecision = 10 ** decimalPlaces;

    do {
      bytes /= threshold;
      ++index;
    } while (
      Math.round(Math.abs(bytes) * roundingPrecision) / roundingPrecision >=
        threshold &&
      index < units.length - 1
    );

    return `${bytes.toFixed(decimalPlaces)} ${units[index]}`;
  }
}
