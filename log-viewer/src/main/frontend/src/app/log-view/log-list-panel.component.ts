import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {LogFile, RestStatus} from './log-file';

@Component({
    selector: 'sl-log-list-panel',
    templateUrl: './log-list-panel.template.html',
    styleUrls: ['./log-list-panel.style.scss']
})
export class LogListPanelComponent implements OnChanges {
    @Input()
    logs: LogFile[];
    @Input()
    statuses: { [key: string]: RestStatus } = {};

    showNodeName: boolean;

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
