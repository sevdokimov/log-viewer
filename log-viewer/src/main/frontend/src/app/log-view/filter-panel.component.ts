import {ChangeDetectorRef, Component, ElementRef, EventEmitter, Output} from '@angular/core';
import {ViewConfigService} from './view-config.service';
import {GroovyPredicate} from './predicates';
import * as $ from 'jquery';
import {Filter, FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';

@Component({
    selector: 'sl-filter-panel',
    templateUrl: './filter-panel.template.html',
    styleUrls: ['./filter-panel.style.scss']
})
export class FilterPanelComponent extends FilterEditorComponent {

    filters: Filter[] = [];

    forceEditingIndex: number;

    @Output()
    apply = new EventEmitter<boolean>();

    constructor(private changeDetectorRef: ChangeDetectorRef,
                private viewConfig: ViewConfigService,
                filterPanelStateService: FilterPanelStateService,
                private el: ElementRef) {
        super(filterPanelStateService);
    }

    protected loadComponentState(state: FilterState) {
        if (state.namedFilters) {
            this.filters = JSON.parse(JSON.stringify(state.namedFilters));
        }  else {
            this.filters = [];
        }
        
        this.forceEditingIndex = null;
    }

    addFilter() {
        this.filters.push({
            predicate: <GroovyPredicate>{type: 'GroovyPredicate', script: ''}
        });
        this.forceEditingIndex = this.filters.length - 1;
    }

    revertChanges() {
        this.loadComponentState(this.filterPanelStateService.getFilterState());
        this.apply.emit(false);
    }

    applyChanges() {
        this.filterPanelStateService.updateFilterState(state => {
            if (this.filters.length === 0 && !state.namedFilters) {
                return;
            }

            state.namedFilters = JSON.parse(JSON.stringify(this.filters));
        });
        this.forceEditingIndex = null;
        
        this.apply.emit(true);
    }

    setFocusOnCheckbox(idx: number) {
        let cb = $('input.enableCb', this.el.nativeElement);
        $(cb[idx]).focus();
    }
}
