import {Component} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';

@Component({
    selector: 'lv-exception-only',
    templateUrl: './exception-only.component.html',
    styleUrls: ['./exception-only.component.scss']
})
export class ExceptionOnlyComponent extends FilterEditorComponent {

    selected: boolean;

    constructor(filterPanelStateService: FilterPanelStateService) {
        super(filterPanelStateService);
    }

    protected loadComponentState(state: FilterState) {
        this.selected = state.exceptionsOnly;
    }

    onClick() {
        this.selected = !this.selected;

        this.filterPanelStateService.updateFilterState(state => {
            state.exceptionsOnly = this.selected ? true : undefined;
        });
    }
}
