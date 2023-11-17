import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationsManageDetailsComponent } from './configurations-manage-details.component';

describe('ConfigurationsManageDetailsComponent', () => {
  let component: ConfigurationsManageDetailsComponent;
  let fixture: ComponentFixture<ConfigurationsManageDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConfigurationsManageDetailsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigurationsManageDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
