import { Component, Input } from '@angular/core';
import { ConfigurationMessage, MessageLog } from '../../../app.service';
import { NgClass } from '@angular/common';
import { ToDateDirective } from '../../../components/to-date.directive';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faChevronDown, faChevronUp } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-configuration-messages',
  imports: [NgClass, ToDateDirective, FaIconComponent],
  templateUrl: './configuration-messages.component.html',
  styleUrl: './configuration-messages.component.scss',
})
export class ConfigurationMessagesComponent {
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration = '';
  protected msgBoxExpanded = false;
  protected readonly faChevronUp = faChevronUp;
  protected readonly faChevronDown = faChevronDown;

  protected getMessageLog(selectedConfiguration: string): ConfigurationMessage[] {
    return this.messageLog[selectedConfiguration]?.messages ?? [];
  }
}
