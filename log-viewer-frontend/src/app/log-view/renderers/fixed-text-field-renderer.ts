import {FieldRenderer} from '@app/log-view/renderers/renderer';
import {SlStyle} from '@app/log-view/renderers/style';
import {SlUtils} from '@app/utils/utils';
import {Record} from '@app/log-view/record';

export class FixedTextFieldRenderer implements FieldRenderer {

    constructor(private map: { [key: string]: SlStyle }) {
    }

    append(e: HTMLElement, s: string, record: Record): void {
        let style = this.map[s];
        if (!style) {
            e.append(s);
        } else {
            let span = document.createElement('SPAN');
            SlUtils.applyStyle(span, style);
            span.innerText = s;
            e.append(span);
        }
    }
}
