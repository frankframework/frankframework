import { Directive, EventEmitter, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { fixedPointFloat } from '../utils';

type WidthHeight = {
  w: number;
  h: number;
};

type Point = {
  x: number;
  y: number;
};

type ViewBox = Point & WidthHeight;

@Directive({
  selector: '[appZoomPan]',
})
export class ZoomPanDirective implements OnChanges, OnDestroy {
  @Input() appZoomPan?: SVGSVGElement | null;
  @Output() newScale: EventEmitter<number> = new EventEmitter();

  private svg: SVGSVGElement | null = null;
  private svgSize: WidthHeight = { w: 0, h: 0 };
  private viewBox: ViewBox = { x: 0, y: 0, w: 0, h: 0 };
  private contentSize: WidthHeight = { w: 0, h: 0 };
  private isPanning: boolean = false;
  private startPoint: Point = { x: 0, y: 0 };
  private endPoint: Point = { x: 0, y: 0 };
  private scaleFactor = 10;
  private zoom: number = 1;
  private observer: ResizeObserver | null = null;

  ngOnChanges(): void {
    if (!!this.appZoomPan) {
      this.svg = this.appZoomPan!;
      this.svg.classList.add('moveable');
      this.contentSize = {
        w: this.svg.viewBox.baseVal.width,
        h: this.svg.viewBox.baseVal.height,
      };
      // this.initialSize();

      // Listen for resizing on this.svg on trigger, call this.resize
      this.observer ??= new ResizeObserver(() => this.resize());
      this.observer.observe(this.svg);

      this.svg.addEventListener('wheel', (event: WheelEvent) => {
        event.preventDefault();
        this.scale(event.offsetX, event.offsetY, Math.sign(event.deltaY));
      });

      this.svg.addEventListener('mousedown', (event: MouseEvent) => {
        if (event.button === 0 && event.target === this.svg) {
          this.isPanning = true;
          this.startPoint = { x: event.pageX, y: event.pageY };
          // document.body.classList.add('prevent-selection');
        }
      });

      document.addEventListener('mousemove', (event: MouseEvent) => {
        if (this.isPanning) this.pan(event);
      });

      document.addEventListener('mouseup', (event: MouseEvent) => {
        if (this.isPanning && event.button === 0) {
          this.viewBox = this.pan(event);
          this.isPanning = false;
          // document.body.classList.remove('prevent-selection');
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private resize(): void {
    if (!this.svg) return;

    const w = this.svg.clientWidth;
    const h = this.svg.clientHeight;
    if (w !== this.svgSize.w || h !== this.svgSize.h) {
      let nw = w;
      let nh = h;
      if (this.svgSize.w !== 0 && this.viewBox.w !== 0) {
        nw = (this.viewBox.w / this.svgSize.w) * w;
      }
      if (this.svgSize.h !== 0 && this.viewBox.h !== 0) {
        nh = (this.viewBox.h / this.svgSize.h) * h;
      }
      this.viewBox = {
        x: -(w / 2 - this.contentSize.w / 2),
        y: -(h / 2 - this.contentSize.h / 2),
        w: fixedPointFloat(nw),
        h: fixedPointFloat(nh),
      };
      this.applyViewBox(this.viewBox);
      this.svgSize = { w, h };
    }
  }

  private scale(mx: number, my: number, direction: number): void {
    this.setScaleFactor(this.scaleFactor + direction * 0.4);
    // viewBox size delta
    const dw = this.viewBox.w - this.svgSize.w * this.zoom;
    const dh = this.viewBox.h - this.svgSize.h * this.zoom;
    // viewBox offset delta
    const dx = (dw * mx) / this.svgSize.w;
    const dy = (dh * my) / this.svgSize.h;
    this.viewBox = {
      x: fixedPointFloat(this.viewBox.x + dx),
      y: fixedPointFloat(this.viewBox.y + dy),
      w: fixedPointFloat(this.viewBox.w - dw),
      h: fixedPointFloat(this.viewBox.h - dh),
    };
    this.applyViewBox(this.viewBox);
  }

  private pan(event: MouseEvent): ViewBox {
    this.endPoint = { x: event.pageX, y: event.pageY };
    // viewBox offset delta
    const dx = (this.startPoint.x - this.endPoint.x) * this.zoom;
    const dy = (this.startPoint.y - this.endPoint.y) * this.zoom;
    const movedViewBox = {
      x: fixedPointFloat(this.viewBox.x + dx),
      y: fixedPointFloat(this.viewBox.y + dy),
      w: this.viewBox.w,
      h: this.viewBox.h,
    };
    this.applyViewBox(movedViewBox);
    return movedViewBox;
  }

  private applyViewBox(viewBox: ViewBox): void {
    this.svg?.setAttribute('viewBox', `${viewBox.x} ${viewBox.y} ${viewBox.w} ${viewBox.h}`);
  }

  private setScaleFactor(factor: number): void {
    this.scaleFactor = Math.max(0, factor);
    this.zoom = fixedPointFloat((this.scaleFactor * this.scaleFactor) / 100); // fixed to 3 decimals to mitigate float rounding errors
    this.newScale.emit(this.zoom * 100);
  }
}
