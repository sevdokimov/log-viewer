import {Component, Input, TemplateRef} from '@angular/core';
import {RestStatus} from '@app/log-view/log-file';
import {BsModalRef, BsModalService} from 'ngx-bootstrap/modal';

@Component({
    selector: 'lv-file-status',
    templateUrl: './file-status.template.html',
    styleUrls: ['./file-status.style.scss']
})
export class FileStatusComponent {

    @Input() status: RestStatus;

    @Input() showErrorMessages: boolean;

    modalRef: BsModalRef;

    constructor(private modalService: BsModalService) {
    }

    showDialog(template: TemplateRef<any>) {
        this.modalRef = this.modalService.show(template, {class: 'modal-lg', animated: false});
    }
}
