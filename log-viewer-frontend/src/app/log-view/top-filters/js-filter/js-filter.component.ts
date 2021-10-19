import {Component, ElementRef, Input, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState, JsFilter} from '@app/log-view/filter-panel-state.service';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import {ViewConfigService} from '@app/log-view/view-config.service';
import {LvUtils} from '@app/utils/utils';
import {AceEditorDirective} from '@app/utils/ace-editor.directive';

@Component({
    selector: 'lv-js-filter',
    templateUrl: './js-filter.component.html',
    styleUrls: ['./js-filter.component.scss', '../disabled-filter.scss'],
})
export class LvJsFilterComponent extends FilterWithDropdown {

    @ViewChild(AceEditorDirective)
    private aceEditor: AceEditorDirective;

    @ViewChild('panelDropDown', {static: true})
    private dropDownElement: ElementRef;

    @Input() filterId: string;

    scriptEditorOptions = {
        maxLines: 20,
        minLines: 5,
        showPrintMargin: false,
        highlightActiveLine: false,
        highlightGutterLine: false,
        setShowGutter: false,
        showLineNumbers: false,
        showGutter: false,
        fixedWidthGutter: false,
        fontSize: 14,
    };

    titleName: string;
    titleScript: string;

    script: string;
    originalScript: string;
    name: string;
    originalName: string;

    enabled: boolean = true;

    fieldList: string[];

    constructor(filterPanelStateService: FilterPanelStateService, private viewConfig: ViewConfigService) {
        super(filterPanelStateService);
    }

    ngOnInit(): void {
        super.ngOnInit();

        let res = {};

        for (let l of Object.values(this.viewConfig.logById)) {
            for (let field of l.fields) {
                res[field.name] = true;
            }
        }

        this.fieldList = Object.keys(res);
    }

    protected getDropdownDiv(): ElementRef {
        return this.dropDownElement;
    }

    private findFilter(state: FilterState) {
        return state.jsFilters?.find(it => it.id === this.filterId);
    }

    protected loadComponentState(state: FilterState) {
        let f = this.findFilter(state);
        if (!f) {
            return;
        }

        this.name = f.name || '';
        this.originalName = this.name;

        this.titleName = LvUtils.trimText(this.name, 50);

        this.script = f.script || '';
        this.originalScript = this.script;

        if (!this.titleName) {
            let s = this.script.replace(/^(\s+|\/\/.+\n|\/\*[^*]*\*\/)*function(?:\s+[a-zA-Z0-9_$]+)?\([^)]*\)\s*\{\s*return\b/, '');
            s = s.replace(/}(\s+|\/\/.+\n|\/\*[^*]*\*\/)*$/, '');
            this.titleScript = LvUtils.trimText(s, 40);
        }

        this.enabled = !f.disabled;
    }

    private saveText(f: JsFilter) {
        f.name = LvUtils.empty2undefined(this.name?.trim());
        f.script = this.script;
    }

    onApply() {
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

    removeFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            let f = this.findFilter(state);
            LvUtils.delete(state.jsFilters, f);
        });
    }

    onScriptChanged(newText: string) {
        this.script = newText;
    }

    editorKeyDown(event: KeyboardEvent) {
        switch (event.keyCode) {
            case 13:
                if (event.ctrlKey) {
                    this.onApply();
                    break;
                }
                return true;

            default:
                return true;
        }

        event.preventDefault();
        return false;
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
