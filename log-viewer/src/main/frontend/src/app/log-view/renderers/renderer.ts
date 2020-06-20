import {Record} from '@app/log-view/record';

export type Text2HtmlConverter = (e: HTMLElement,
                                  type: string,
                                  s: string) => void;

export interface Highlight {
    start: number;
    end: number;
    e: HTMLElement;
}

export interface FieldRenderer {
    append(e: HTMLElement,
           s: string,
           record: Record,
           textRenderer?: Text2HtmlConverter,
    ): void;
}

export interface TextRenderer {
    tryRender(s: string, textRenderer?: Text2HtmlConverter): Highlight[];
}
