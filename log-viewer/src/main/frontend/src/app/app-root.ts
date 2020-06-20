import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';

var providers: any = [Title];

@Component({
    selector: 'app-root',
    template: `
        <global-nav *ngIf="showGlobNav"></global-nav>
        <router-outlet></router-outlet>`,
    providers: providers
})
export class AppRoot implements OnInit {
    showGlobNav: boolean;

    // notificationOptions = {
    //     position:['top', 'right'],
    //     showProgressBar: false,
    //     timeOut: 5000
    // };

    constructor(viewContainer: ViewContainerRef,
                private route: ActivatedRoute,
                private router: Router) {
    }

    ngOnInit() {
    }
}
