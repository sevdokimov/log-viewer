import {Component, Input, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {LevelFilterDescription} from '@app/log-view/top-filters/level-list/LevelFilterDescription';
import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';
import {BsDropdownDirective} from 'ngx-bootstrap';

@Component({
    selector: 'lv-level-list',
    templateUrl: './level-list.component.html',
    styleUrls: ['./level-list.component.scss']
})
export class LevelListComponent extends FilterEditorComponent {

    @ViewChild('levelDd', {read: BsDropdownDirective, static: true})
    private levelDropDown: BsDropdownDirective;

    @Input() levelsDescription: LevelFilterDescription;

    selected: { [key: string]: boolean } = {};

    maxLevel: string;
    hasSkipped: boolean;

    private selectUsingMouse: {[key: string]: boolean} = {};

    private hoverLevel: string;

    constructor(filterPanelStateService: FilterPanelStateService) {
        super(filterPanelStateService);
    }

    protected loadComponentState(state: FilterState) {
        let levels: string[] = state.level;

        for (let level of this.levelsDescription.levels) {
            this.selected[level] = (!levels || levels.length === 0 || levels.includes(level));
        }

        this.initMaxLevel();
    }

    private saveComponentState() {
        this.filterPanelStateService.updateFilterState(state => {
            if (!state.level) {
                state.level = [];
            }

            for (let level of this.levelsDescription.levels) {
                let idx = state.level.indexOf(level);

                if (this.selected[level]) {
                    if (idx < 0) {
                        state.level.push(level);
                    }
                } else {
                    if (idx >= 0) {
                        state.level.splice(idx, 1);
                    }
                }
            }
        });
    }

    private initMaxLevel() {
        if (this.levelsDescription.levels.length === 0) {
            this.maxLevel = null;
            this.hasSkipped = false;
            return;
        }

        let maxSelectedIdx: number = null;

        for (let i = 0; i < this.levelsDescription.levels.length; i++) {
            if (this.selected[this.levelsDescription.levels[i]]) {
              maxSelectedIdx = i;
            }
        }

        if (maxSelectedIdx == null) {
            this.maxLevel = this.levelsDescription.levels[-1];
            this.hasSkipped = false;
            return;
        }

        this.maxLevel = this.levelsDescription.levels[maxSelectedIdx];

        this.hasSkipped = this.levelsDescription.levels.findIndex((level, idx) => {
            return !this.selected[level] || idx === maxSelectedIdx;
        }) < maxSelectedIdx;
    }

    onCheckboxSelected() {
        this.initMaxLevel();
        this.saveComponentState();
    }

    onLevelSelected(level: string) {
        if (level === this.maxLevel && !this.hasSkipped) {
            return; // no changed.
        }

        let val = true;

        for (const l of this.levelsDescription.levels) {
            this.selected[l] = val;

            if (l === level) {
                val = false;
            }
        }

        this.initMaxLevel();
        this.saveComponentState();

        this.levelDropDown.hide();
    }

    onLevelMouseEnter(level: string) {
        this.hoverLevel = level;

        let val = true;
        for (const l of this.levelsDescription.levels) {
            this.selectUsingMouse[l] = val;

            if (l === level) {
                val = false;
            }
        }
    }

    onLevelMouseLeave(level: string) {
        if (this.hoverLevel !== level) {
            return;
        }

        this.selectUsingMouse = {};
        this.hoverLevel = null;
    }
}
