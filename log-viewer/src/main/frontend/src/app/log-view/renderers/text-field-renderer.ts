import {FieldRenderer, Text2HtmlConverter} from './renderer';
import {SlUtils} from '@app/utils/utils';
import {SlStyle} from './style';
import {Record} from '@app/log-view/record';

export class TextFieldRenderer implements FieldRenderer {
    private style: SlStyle;

    private textType: string;

    constructor(cfg: TextFieldRendererCfg = {}) {
        this.style = cfg.style;
        this.textType = cfg.textType || 'text';
    }

    append(e: HTMLElement, s: string, record: Record, textRenderer: Text2HtmlConverter) {
        if (this.style) {
            let span = document.createElement('SPAN');
            SlUtils.applyStyle(span, this.style);
            textRenderer(span, this.textType, s);
            e.appendChild(span);
        } else {
            textRenderer(e, this.textType, s);
        }
    }
}

export interface TextFieldRendererCfg {
    style?: SlStyle;

    textType?: string;
}
