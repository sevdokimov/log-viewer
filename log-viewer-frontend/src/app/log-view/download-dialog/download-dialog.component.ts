import {AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {LogFile} from '@app/log-view/log-file';
import {ViewStateService} from '@app/log-view/view-state.service';
import {LvUtils} from '@app/utils/utils';
import {Predicate} from "@app/log-view/predicates";
import {Md5} from "ts-md5";

@Component({
    selector: 'lv-download-dialog',
    templateUrl: './download-dialog.template.html',
    styleUrls: ['./download-dialog.style.scss', '../log-table.scss']
})
export class DownloadDialogComponent implements OnInit, AfterViewInit {

    @ViewChild('fileNameElement', {static: true})
    fileNameElement: ElementRef;

    @Input() logs: LogFile[];

    @Input() effectiveFilters: Predicate[];

    downloadFlag: {[key: string]: boolean} = {};

    showNodeName: boolean;

    logsSorted: LogFile[];

    fileName: string;

    effectiveFiltersJson: string;

    selectedLogsCount: number;

    zip: boolean;

    errorMsg: string;

    constructor(public vs: ViewStateService) {
    }

    ngAfterViewInit(): void {
        this.fileNameElement.nativeElement.focus();
        setTimeout(() => {
            let input = <HTMLInputElement>this.fileNameElement.nativeElement;
            if (input.value.toLowerCase().endsWith('.log')) {
                input.setSelectionRange(0, input.value.length - '.log'.length)
            } else {
                input.select();
            }
        }, 0);
    }

    ngOnInit(): void {
        this.logsSorted = [...this.logs].sort((a, b) => {
            let res = a.path.localeCompare(b.path);
            if (res)
                return res;

            return a.node.localeCompare(b.node);
        });

        this.effectiveFiltersJson = JSON.stringify(this.effectiveFilters);

        this.showNodeName = this.logs.length > 1 && this.logs.some(l => l.node !== this.logs[0].node);

        for (let l of this.logsSorted) {
            this.downloadFlag[l.id] = this.isValid(l);
        }

        this.synchronizeSelectedLogsCount();

        this.fileName = this.generateName()
    }

    synchronizeSelectedLogsCount() {
        let logsCount = 0;
        for (let flag of Object.values(this.downloadFlag)) {
            if (flag)
                logsCount++;
        }

        this.selectedLogsCount = logsCount;

        if (logsCount > 1)
            this.zip = true;
    }

    isValid(log: LogFile) {
        return !this.vs.statuses[log.id].errorType;
    }

    private generateName(): string {
        let names: string[] = this.logs.filter(l => this.isValid(l)).map(l => LvUtils.extractName(l.path).toLowerCase());

        let uniqueNames = Array.from(new Set(names).keys()).sort()

        if (uniqueNames.length === 1) {
            return uniqueNames[0];
        }

        if (uniqueNames.length < 5) {
            return names.map(name => name.toLowerCase().endsWith('.log') ? name.substring(0, name.length - 4) : name)
                .join('+') + '.log'
        }

        return Md5.hashStr(names.join('+')).toString().substring(0, 8) + '.log'
    }

    download() {
        return !(!this.fileName || this.selectedLogsCount === 0);
    }
}
