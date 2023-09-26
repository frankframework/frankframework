import { Component, EventEmitter, Output } from '@angular/core';
import { StateService } from '@uirouter/angularjs';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss']
})
export class PagesNavigationComponent {
  routeState = this.$state
  @Output() onOpenInfo = new EventEmitter<void>();
  @Output() onOpenFeedback = new EventEmitter<void>();

  constructor(private $state: StateService) { }

  openInfo(){
    this.onOpenInfo.emit();
  }

  openFeedback(){
    this.onOpenFeedback.emit();
  }

  getClassByRoute(className: string, routeState: string){
    return { [className]: this.$state.includes(routeState) }
  }
}
