import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';
import {ElementRef} from '@angular/core';
import * as $ from 'jquery';

export abstract class FilterWithDropdown extends FilterEditorComponent {

    dropdownShown: boolean;
    dropdownRight: boolean;

    protected abstract getDropdownDiv(): ElementRef;

    toggleFilterPanel() {
        if (this.dropdownShown) {
            this.dropdownShown = false;
        } else {
            if (this.filterPanelStateService.openedDropdown) {
                this.filterPanelStateService.openedDropdown.dropdownShown = false;
            }

            this.dropdownShown = true;

            this.filterPanelStateService.openedDropdown = this;

            let rect = this.getDropdownDiv().nativeElement.getBoundingClientRect();
            this.dropdownRight = rect.x + rect.width > 400;

            setTimeout(() => {
                if (this.dropdownShown) {
                    let rect = $('.lv-dropdown-panel', this.getDropdownDiv().nativeElement)[0].getBoundingClientRect();
                    if (rect.x < 0) {
                        this.dropdownRight = false;
                    }
                }
            });
        }
    }
}
