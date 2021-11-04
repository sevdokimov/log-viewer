import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {LogFile} from './log-file';
import {ViewStateService} from "@app/log-view/view-state.service";

@Component({
    selector: 'lv-log-list-panel',
    templateUrl: './log-list-panel.template.html',
    styleUrls: ['./log-list-panel.style.scss', './log-table.scss']
})
export class LogListPanelComponent implements OnChanges {
    @Input()
    logs: LogFile[];

    @Output() addLogClicked = new EventEmitter();

    showNodeName: boolean;

    constructor(public vs: ViewStateService) {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['logs']) {
            this.showNodeName = false;

            if (this.logs.length > 1) {
                let firstNode = this.logs[0].node;
                this.showNodeName = this.logs.some(l => l.node !== firstNode);
            }
        }
    }
}
