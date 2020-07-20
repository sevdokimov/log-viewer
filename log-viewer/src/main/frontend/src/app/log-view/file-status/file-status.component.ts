import {ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {ViewConfigService} from '../view-config.service';
import {Record} from '@app/log-view/record';
import {LogFile, RestStatus} from '@app/log-view/log-file';
import {ViewStateService} from '@app/log-view/view-state.service';

@Component({
    selector: 'sl-file-status',
    templateUrl: './file-status.template.html',
    styleUrls: ['./file-status.style.scss']
})
export class FileStatusComponent {

    @Input() status: RestStatus;

    @Input() showErrorMessages: boolean;
}
