import {Highlight, TextRenderer} from '@app/log-view/renderers/renderer';
import {LvUtils} from '@app/utils/utils';
import {SlStyle} from '@app/log-view/renderers/style';

export class RegexHighlighter implements TextRenderer {

    private readonly regex: RegExp;

    private readonly style: SlStyle;

    constructor(private cfg: {regex: string, style: SlStyle }) {
        this.regex = new RegExp(cfg.regex, 'g');

        LvUtils.assert(cfg.style != null);
        this.style = cfg.style;
    }

    tryRender(s: string): Highlight[] {
        this.regex.lastIndex = 0;

        let res: Highlight[] = [];

        while (true) {
            let matcher = this.regex.exec(s);
            if (!matcher) {
                return res;
            }

            let e = document.createElement('SPAN');
            e.textContent = matcher[0];
            LvUtils.applyStyle(e, this.style);

            res.push({start: matcher.index, end: this.regex.lastIndex, e});
        }
    }
}
