import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';

const providers: any = [Title];

@Component({
    selector: 'app-root',
    template: `
        <global-nav *ngIf="showGlobNav"></global-nav>
        <router-outlet></router-outlet>`,
    providers: providers
})
export class AppRoot {
    showGlobNav: boolean;

    // notificationOptions = {
    //     position:['top', 'right'],
    //     showProgressBar: false,
    //     timeOut: 5000
    // };

}
