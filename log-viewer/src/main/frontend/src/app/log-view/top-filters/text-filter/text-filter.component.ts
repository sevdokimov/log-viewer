import {Component, ElementRef, Input, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState, TextFilter} from '@app/log-view/filter-panel-state.service';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import {SlUtils} from '@app/utils/utils';
import {SearchPattern, SearchUtils} from '@app/log-view/search';

@Component({
    selector: 'lv-text-filter',
    templateUrl: './text-filter.component.html',
    styleUrls: ['./text-filter.component.scss', '../disabled-filter.scss'],
})
export class LvTextFilterComponent extends FilterWithDropdown {

    @ViewChild('panelDropDown', {static: true})
    private dropDownElement: ElementRef;

    @ViewChild('textInput', {static: false})
    private textInput: ElementRef;

    @Input() filterId: string;

    enabled: boolean = true;

    titleName: string;
    titleText: string;

    name: string;
    originalName: string;

    pattern: SearchPattern;
    patternOriginal: SearchPattern;

    includeExclude: string;

    regexpError: string;

    constructor(filterPanelStateService: FilterPanelStateService) {
        super(filterPanelStateService);
    }

    protected getDropdownDiv(): ElementRef {
        return this.dropDownElement;
    }

    private findFilter(state: FilterState) {
        return state.textFilters?.find(it => it.id === this.filterId);
    }

    hasChanges() {
        return this.name !== this.originalName || !SearchUtils.equals(this.pattern, this.patternOriginal);
    }

    protected loadComponentState(state: FilterState) {
        let f = this.findFilter(state);
        if (!f) {
            return;
        }

        this.name = f.name || '';
        this.originalName = this.name;

        this.pattern = Object.assign({s: ''}, f.pattern);
        this.patternOriginal = Object.assign({}, f.pattern);

        this.includeExclude = f.exclude ? 'exclude' : 'include';

        this.titleName = SlUtils.trimText(this.name, 50);

        if (!this.titleName) {
            this.titleText = SlUtils.trimText(this.pattern.s, 40);
            
            if (!this.titleText) {
                this.titleText = '<empty>';
            }
        }

        this.checkRegexp();

        this.enabled = !f.disabled;
    }

    private saveText(f: TextFilter) {
        f.name = this.name?.trim();
        f.pattern = Object.assign({}, this.pattern);
        f.exclude = this.includeExclude === 'exclude';
    }

    onApply() {
        if (this.regexpError) {
            return false;
        }

        this.filterPanelStateService.updateFilterState(state => {
            let f = this.findFilter(state);
            if (!f) {
                return;
            }

            this.saveText(f);
        });

        this.dropdownShown = false;
    }

    onCancel() {
        this.dropdownShown = false;
        this.loadComponentState(this.filterPanelStateService.getFilterState());
    }

    toggleFilterPanel() {
        super.toggleFilterPanel();

        if (this.dropdownShown) {
            setTimeout(() => this.textInput?.nativeElement?.focus(), 0);
        }
    }

    removeFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            let f = this.findFilter(state);
            SlUtils.delete(state.textFilters, f);
        });
    }

    editorKeyDown(event: KeyboardEvent) {
        if (event.keyCode === 13 && event.ctrlKey) {
            this.onApply();
            event.preventDefault();
            return false;
        }
    }

    checkRegexp() {
        if (!this.pattern.regex) {
            this.regexpError = null;
        } else {
            try {
                // tslint:disable-next-line:no-unused-expression
                new RegExp(this.pattern.s);
                this.regexpError = null;
            } catch (e) {
                this.regexpError = e.message || 'Invalid regexp';
            }
        }
    }

    enableDisable() {
        this.filterPanelStateService.updateFilterState(state => {
            let f = this.findFilter(state);
            if (!f) {
                return;
            }

            f.disabled = !this.enabled;
            this.saveText(f);
        });
    }
}
