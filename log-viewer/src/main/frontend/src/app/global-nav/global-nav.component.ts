import {Component} from '@angular/core';
import {Router} from '@angular/router';

@Component({
    selector: 'global-nav',
    templateUrl: './global-nav.template.html',
    styleUrls: ['./global-nav.style.scss']
})
export class GlobalNavigation {
    constructor(private router: Router) {
    }
}
