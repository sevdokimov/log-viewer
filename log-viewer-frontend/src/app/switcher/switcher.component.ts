import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostBinding,
    HostListener,
    Input,
    Output,
} from '@angular/core';
import {ControlValueAccessor} from '@angular/forms';

@Component({
    selector: 'lv-switch',
    templateUrl: './switcher.template.html',
    styleUrls: ['./switcher.style.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SwitcherComponent implements ControlValueAccessor {
    @Input() value: boolean = false;
    @Input() @HostBinding('class.reversed') reversed: boolean = false;
    @Input() @HostBinding('class.disabled') disabled: boolean = false;
    @Output() valueChange = new EventEmitter<boolean>();

    @HostBinding('attr.tabindex')
    get tabindex(): number {
        return this.disabled ? null : 0;
    }

    modelChanged = (val: boolean) => {};

    constructor() {
    }

    @HostListener('click', ['$event'])
    onClick(event: MouseEvent) {
        if (this.disabled) {
            return;
        }

        event.stopPropagation();
        this.toggleValue();
    }
    private toggleValue() {
        if (this.disabled) {
            return;
        }

        this.value = !this.value;
        this.modelChanged(this.value);
        this.valueChange.emit(this.value);
    }

    @HostListener('keydown.Space', ['$event'])
    onSpace(event: KeyboardEvent) {
        event.preventDefault();

        this.toggleValue();
    }

    registerOnChange(cb: (val: boolean) => void): void {
        this.modelChanged = cb;
    }

    registerOnTouched(cb: Function): void {
    }

    writeValue(value: boolean): void {
        this.value = value;
        this.valueChange.emit(value);
    }
}
