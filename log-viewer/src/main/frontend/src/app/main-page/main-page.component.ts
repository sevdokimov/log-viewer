import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {OpenEvent} from '@app/log-navigator/log-navigator.component';

@Component({
    selector: 'sl-main-page',
    templateUrl: './main-page.template.html',
    styleUrls: ['./main-page.style.scss'],
})
export class MainPageComponent {

    constructor(private router: Router) {

    }

    open(event: OpenEvent) {
        if (event.isCtrlClick) {
            window.open('log?path=' + encodeURI(event.path));
        } else {
            this.router.navigate(['/log'], {
                queryParams: {path: event.path}
            });
        }
    }

}
