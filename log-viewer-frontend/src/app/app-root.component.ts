import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {TranslateService} from "@ngx-translate/core";
import {environment} from "@env/environment";

const providers: any = [Title];

@Component({
    selector: 'app-root',
    template: `<router-outlet></router-outlet>`,
    providers: providers
})
export class AppRootComponent {
    constructor(translate: TranslateService) {
        translate.setDefaultLang(environment.defaultLocale);

        const language = localStorage.getItem('language');

        if (language && language.length > 0) {
            translate.use(language);
        } else {
            translate.use(navigator.language);
        }
    }
}
