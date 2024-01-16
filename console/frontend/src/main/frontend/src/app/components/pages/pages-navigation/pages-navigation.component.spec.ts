import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesNavigationComponent } from './pages-navigation.component';

describe('PagesNavigationComponent', () => {
  let component: PagesNavigationComponent;
  let fixture: ComponentFixture<PagesNavigationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PagesNavigationComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PagesNavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
