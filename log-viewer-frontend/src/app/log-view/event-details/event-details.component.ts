import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnChanges,
    QueryList,
    SimpleChanges,
    ViewChildren
} from '@angular/core';
import {FieldDescr, ViewConfigService} from '../view-config.service';
import {Record} from '@app/log-view/record';
import {LogFile} from '@app/log-view/log-file';
import {ViewStateService} from '@app/log-view/view-state.service';
import {SlUtils} from '@app/utils/utils';
import {RecordRendererService} from '@app/log-view/record-renderer.service';

@Component({
    selector: 'sl-event-details',
    templateUrl: './event-details.template.html',
    styleUrls: ['./event-details.style.scss']
})
export class EventDetailsComponent implements OnChanges, AfterViewInit {

    @ViewChildren('fieldVal') fieldValues: QueryList<ElementRef>;

    @Input() record: Record;

    log: LogFile;

    timestamp: string;

    fields: {field: FieldDescr, html: HTMLElement}[];

    constructor(private changeDetectorRef: ChangeDetectorRef,
                public vs: ViewStateService,
                private recRenderer: RecordRendererService,
                private viewConfig: ViewConfigService) {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['record']) {
            let r = this.record;

            this.log = this.viewConfig.logById[r.logId];

            this.timestamp = r.time ? SlUtils.formatDate(r.time) : null;

            this.fields = [];

            if (this.log) {
                for (let field of this.log.fields) {
                    let e: HTMLElement = document.createElement('SPAN');
                    e.className = 'rec-text';

                    this.recRenderer.renderField(e, r, field);

                    this.fields.push({field, html: e});
                }
            }
        }
    }

    fieldClick(event: MouseEvent) {
        this.recRenderer.handleClick(event);
    }

    ngAfterViewInit() {
        let fieldValues = this.fieldValues.toArray();

        SlUtils.assert(fieldValues.length === this.fields.length);

        for (let i = 0; i < this.fields.length; i++) {
            fieldValues[i].nativeElement.append(this.fields[i].html);
        }
    }
}
