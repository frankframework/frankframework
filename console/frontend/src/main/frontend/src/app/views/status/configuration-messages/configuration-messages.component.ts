import { Component, Input } from '@angular/core';
import { AdapterMessage, MessageLog } from '../../../app.service';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { ToDateDirective } from '../../../components/to-date.directive';

@Component({
  selector: 'app-configuration-messages',
  imports: [NgIf, NgClass, NgFor, ToDateDirective],
  templateUrl: './configuration-messages.component.html',
  styleUrl: './configuration-messages.component.scss',
})
export class ConfigurationMessagesComponent {
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration: string = '';
  protected msgBoxExpanded = false;

  protected getMessageLog(selectedConfiguration: string): AdapterMessage[] {
    return this.messageLog[selectedConfiguration]?.messages ?? [];
  }
}
