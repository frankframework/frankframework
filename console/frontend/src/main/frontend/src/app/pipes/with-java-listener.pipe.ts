import { Pipe, PipeTransform } from '@angular/core';
import { Adapter } from '../app.service';

@Pipe({
  name: 'withJavaListener',
  standalone: false,
})
export class WithJavaListenerPipe implements PipeTransform {
  transform(adapters: Record<string, Adapter>): Adapter[] {
    if (!adapters) return [];
    // let schedulerEligibleAdapters: Record<string, Adapter> = {};
    const schedulerEligibleAdapters: Adapter[] = [];
    for (const adapter in adapters) {
      const receivers = adapters[adapter].receivers ?? [];
      for (const receiver of receivers) {
        if (receiver.listener.class.startsWith('JavaListener')) {
          schedulerEligibleAdapters.push(adapters[adapter]);
          break;
        }
      }
    }
    return schedulerEligibleAdapters;
  }
}
