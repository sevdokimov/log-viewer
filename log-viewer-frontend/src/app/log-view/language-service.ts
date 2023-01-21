import {TranslateService} from "@ngx-translate/core";
import {Injectable} from "@angular/core";

@Injectable({providedIn: 'root'})
export class LanguageService {
    constructor(private translate: TranslateService) {
    }

    applyLang(lang: string) {
        this.translate.use(lang);
    }

    is(language: string) {
        return this.translate.currentLang === language;
    }

    getTranslate(key: string, interpolateParams?: Object) {
        let result = '';

        this.translate.get(key, interpolateParams).subscribe(value => {result = value})

        return result;
    }
}