import {Highlight, TextRenderer} from '@app/log-view/renderers/renderer';
import {LvUtils} from '@app/utils/utils';
import {SlStyle} from '@app/log-view/renderers/style';

export class MillisecondRenderer implements TextRenderer {

    private static regex: RegExp = new RegExp(/\b(\d{4,11}) ?ms\b/ig);

    private style: SlStyle;

    constructor(private cfg: { style?: SlStyle }) {
        this.style = cfg ? cfg.style : null;
    }

    tryRender(s: string): Highlight[] {
        MillisecondRenderer.regex.lastIndex = 0;

        let res: Highlight[] = [];

        while (true) {
            let matcher = MillisecondRenderer.regex.exec(s);
            if (!matcher) {
                return res;
            }

            if (s.endsWith('.', matcher.index)) {  // "12.2232ms" - bad string
                continue;
            }

            let e = document.createElement('SPAN');
            e.textContent = matcher[0];
            e.className = 'text-milliseconds';
            e.title = LvUtils.renderTimePeriod(+matcher[1]);
            LvUtils.applyStyle(e, this.style);

            res.push({start: matcher.index, end: MillisecondRenderer.regex.lastIndex, e});
        }
    }
}
