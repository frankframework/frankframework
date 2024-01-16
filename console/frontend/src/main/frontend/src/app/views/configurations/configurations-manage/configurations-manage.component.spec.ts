import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationsManageComponent } from './configurations-manage.component';

describe('ConfigurationsManageComponent', () => {
  let component: ConfigurationsManageComponent;
  let fixture: ComponentFixture<ConfigurationsManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConfigurationsManageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigurationsManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
