import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';
import {ElementRef} from '@angular/core';

export abstract class FilterWithDropdown extends FilterEditorComponent {

    dropdownShown: boolean;
    dropdownRight: boolean;

    protected abstract getDropdownDiv(): ElementRef;

    toggleFilterPanel() {
        if (this.dropdownShown) {
            this.dropdownShown = false;
        } else {
            this.dropdownShown = true;

            let rect = this.getDropdownDiv().nativeElement.getBoundingClientRect();
            this.dropdownRight = rect.x + rect.width > 440;
        }
    }
}
