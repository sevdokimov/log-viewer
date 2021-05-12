import {Component, Input, TemplateRef, ViewChild} from '@angular/core';
import {RestStatus} from '@app/log-view/log-file';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'lv-file-status',
    templateUrl: './file-status.template.html',
    styleUrls: ['./file-status.style.scss']
})
export class FileStatusComponent {

    @ViewChild('stacktraceDialog', {static: true}) public stacktraceDialog: TemplateRef<any>;

    @Input() status: RestStatus;

    @Input() showErrorMessages: boolean;

    constructor(private modalService: NgbModal) {
    }

    showStacktrace() {
        this.modalService.open(this.stacktraceDialog, {size: 'xl'});
    }
}
