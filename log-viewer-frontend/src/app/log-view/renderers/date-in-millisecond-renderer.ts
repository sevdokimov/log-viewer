import {Highlight, TextRenderer} from '@app/log-view/renderers/renderer';
import {SlUtils} from '@app/utils/utils';
import {SlStyle} from '@app/log-view/renderers/style';

export class DateInMillisecondRenderer implements TextRenderer {

    private static regex: RegExp = new RegExp(/\b[12]\d{12}\b/ig);

    private static now: number = Date.now();

    private style: SlStyle;

    constructor(private cfg: {style?: SlStyle}) {
        this.style = cfg ? cfg.style : null;
    }

    tryRender(s: string): Highlight[] {
        DateInMillisecondRenderer.regex.lastIndex = 0;

        let res: Highlight[] = [];

        while (true) {
            let matcher = DateInMillisecondRenderer.regex.exec(s);
            if (!matcher) {
                return res;
            }

            let date = +matcher[0];

            if (date < DateInMillisecondRenderer.now - 7 * 365 * 24 * 60 * 60 * 1000
                || date > DateInMillisecondRenderer.now + 365 * 24 * 60 * 60 * 1000) {
                continue;
            }

            let e = document.createElement('SPAN');
            e.textContent = matcher[0];
            e.className = 'text-date-in-milliseconds';
            e.title = SlUtils.formatDate(date);
            SlUtils.applyStyle(e, this.style);

            res.push({start: matcher.index, end: DateInMillisecondRenderer.regex.lastIndex, e});
        }
    }
}
