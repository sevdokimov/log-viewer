import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';

const providers: any = [Title];

@Component({
    selector: 'app-root',
    template: `<router-outlet></router-outlet>`,
    providers: providers
})
export class AppRootComponent {

}
