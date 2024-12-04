import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TabListComponent } from './tab-list.component';

describe('TabListComponent', () => {
  let component: TabListComponent;
  let fixture: ComponentFixture<TabListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TabListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TabListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
