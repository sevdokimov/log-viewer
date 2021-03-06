import {Highlight, TextRenderer} from './renderer';
import {SlStyle} from './style';
import {LvUtils} from '@app/utils/utils';

export class FixedTextRenderer implements TextRenderer {
    constructor(private map: { [key: string]: SlStyle }) {
    }

    tryRender(s: string): Highlight[] {
        let style = this.map[s];
        if (!style) { return null; }

        let e = document.createElement('SPAN');
        LvUtils.applyStyle(e, style);
        e.innerText = s;

        return [{start: 0, end: s.length, e}];
    }
}
