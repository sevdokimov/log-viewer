import {Injectable} from '@angular/core';
import {FieldRenderer, TextRenderer} from './renderers/renderer';
import {TextFieldRenderer} from './renderers/text-field-renderer';
import {LogFile} from './log-file';
import {PathUtils} from '../utils/path-utils';
import {SlUtils} from '../utils/utils';
import {UiConfig} from '@app/log-view/backend-events';
import {ObjectFactory} from '@app/utils/object-factory';
import {SlStyle} from '@app/log-view/renderers/style';

@Injectable()
export class ViewConfigService {
    logById: { [key: string]: LogFile} = {};

    uiConfig: UiConfig;

    localhostName: string;

    logLabelRenders: { [key: string]: () => HTMLElement };

    private textRenderers: { supportedTypes: string[], renderer: TextRenderer}[] = [];

    private static generateRenders(logs: LogFile[]): { [key: string]: () => HTMLElement } {
        let nodes = logs.map(l => l.node).filter(n => n);
        let nodeLabels = ViewConfigService.generateNodeLabels(nodes);

        let paths = logs.map(l => l.path).filter(p => p);
        let pathRenders = ViewConfigService.generatePathLabels(paths);

        let res: { [key: string]: () => HTMLElement } = {};

        for (let l of logs) {
            let node = l.node ? nodeLabels[l.node] : null;
            let path = pathRenders[l.path];

            let renderer: () => any;

            if (!node && !path) {
                renderer = () => {
                    return null;
                };
            } else {
                let text: string;

                if (!path) {
                    text = node;
                } else {
                    if (node) {
                        text = '[' + node + '] ' + path;
                    } else {
                        text = path;
                    }
                }

                renderer = () => {
                    let div = document.createElement('DIV');
                    div.className = 'log-label';
                    div.textContent = text;

                    if (node) {
                        div.title = '[' + l.node + '] ' + l.path;
                    } else {
                        div.title = l.path;
                    }

                    return div;
                };
            }

            res[l.id] = renderer;
        }

        return res;
    }

    private static generateNodeLabels(nodes: string[]): { [key: string]: string } {
        let res: { [key: string]: string } = {};

        nodes = SlUtils.distinct(nodes);

        if (nodes.length === 0) { return res; }

        if (nodes.length === 1) {
            res[nodes[0]] = null;
            return res;
        }

        let labels = nodes.slice(0);

        function matchesWith(s: string, regex: RegExp, value: string): boolean {
            if (s.length === value.length) { return false; }

            let matcher = regex.exec(s);
            if (!matcher) { return false; }

            return matcher[0] === value;
        }

        // Remove common prefixes
        let prefixRegex = new RegExp(/^(?:[a-zA-Z]+|[0-9]+|[^a-zA-Z0-9]+)/);

        while (true) {
            let matcher = prefixRegex.exec(labels[0]);
            if (!matcher) { break; }

            let prefix = matcher[0];

            if (labels.some(n => !matchesWith(n, prefixRegex, prefix))) { break; }

            labels.forEach((h, idx) => (labels[idx] = h.substr(prefix.length)));
        }

        // Remove common suffix
        let suffixRegex = new RegExp(/(?:[a-zA-Z]+|[0-9]+|[^a-zA-Z0-9]+)$/);

        while (true) {
            let matcher = suffixRegex.exec(labels[0]);
            if (!matcher) { break; }

            let suffix = matcher[0];

            if (labels.some(n => !matchesWith(n, suffixRegex, suffix))) { break; }

            labels.forEach(
                (h, idx) => (labels[idx] = h.substr(0, h.length - suffix.length))
            );
        }

        nodes.forEach((node, i) => (res[node] = labels[i]));

        return res;
    }

    private static generatePathLabels(paths: string[]): { [key: string]: string } {
        let res: { [key: string]: string } = {};

        paths = SlUtils.distinct(paths);

        if (paths.length === 0) { return res; }

        if (paths.length === 1) {
            res[paths[0]] = null;
            return res;
        }

        let shortNameMap: { [key: string]: string[] } = {};

        for (let path of paths) {
            let name = PathUtils.extractName(path);
            if (name.toLowerCase().endsWith('.log')) {
                name = name.substr(0, name.length - '.log'.length);
            }

            let list = shortNameMap[name];
            if (!list) {
                list = [];
                shortNameMap[name] = list;
            }

            list.push(path);
        }

        for (let shortName in shortNameMap) {
            if (shortNameMap.hasOwnProperty(shortName)) {
                let list = shortNameMap[shortName];
                if (list.length === 1) {
                    res[list[0]] = shortName;
                } else {
                    list.forEach(path => (res[path] = path));
                }
            }
        }

        return res;
    }

    setRendererCfg(logs: LogFile[], uiConfig: UiConfig, localhostName: string) {
        this.uiConfig = uiConfig;

        this.localhostName = localhostName;

        this.logLabelRenders = ViewConfigService.generateRenders(
            logs.filter(l => l.connected)
        );

        for (let log of logs) {
            this.logById[log.id] = log;

            for (let i = 0; i < log.fields.length; i++) {
                let field = log.fields[i];

                field._rendererInstance = ViewConfigService.createFieldRenderer(field, uiConfig);
                field._index = i;
            }
        }

        let highlightersCfg = Object.values(uiConfig['text-highlighters']);

        highlightersCfg = highlightersCfg.sort((h1, h2) => {
            return (h1.priority || 0) - (h2.priority || 0);
        });

        for (const highlighter of highlightersCfg) {
            if (highlighter.enabled === false) {
                continue;
            }

            let renderer = ObjectFactory.createRenderer(highlighter.class, highlighter.args);

            this.textRenderers.push({supportedTypes: highlighter['text-type'], renderer});
        }

        this.sendStatisticIfNeeded();
    }

    private sendStatisticIfNeeded() {
        if (!this.uiConfig['send-usage-statistics']) {
            return;
        }

        let date = localStorage.getItem('last-send-usage-statistics');

        // send statistic not often than once per day.
        if (!date || parseInt(date, 10) + 24 * 60 * 60 * 1000 < new Date().getTime()) {
            localStorage.setItem('last-send-usage-statistics', '' + new Date().getTime());

            let fakeImage: HTMLImageElement = document.createElement('img');

            fakeImage.src = 'http://myregexp.com/log-viewer-statistic/0.1.3/sending-usage-statistics_can_be_disabled_by_removing_send-usage-statistics_property_in_the_configuration.png';
            fakeImage.style.position = 'absolute';
            fakeImage.style.left = '-101px';
            fakeImage.style.top = '-101px';
            fakeImage.style.right = '-100px';
            fakeImage.style.bottom = '-100px';

            document.body.appendChild(fakeImage);
        }
    }

    getTextRenderers(): { supportedTypes: string[], renderer: TextRenderer}[] {
        return this.textRenderers;
    }

    private static createFieldRenderer(field: FieldDescr, uiConfig: UiConfig): FieldRenderer {
        let fieldTypeDescription = ViewConfigService.subtypes(field.type).map(type => uiConfig['field-types'][type]).find(x => x);

        if (!fieldTypeDescription) {
            return new TextFieldRenderer();
        }

        if (fieldTypeDescription.class) {
            return ObjectFactory.createRenderer(fieldTypeDescription.class, fieldTypeDescription.args);
        }

        return new TextFieldRenderer({style: fieldTypeDescription.style, textType: fieldTypeDescription.textType || 'text'});
    }

    private static subtypes(type: string): string[] {
        if (!type) {
            return [];
        }

        let res: string[] = [];

        while (true) {
            res.push(type);

            let idx = type.lastIndexOf('/');
            if (idx <= 0) {
                break;
            }

            type = type.substring(0, idx);
        }

        return res;
    }
}

export interface FieldDescr {
    name: string;
    type: string;

    _index: number;
    _rendererInstance: FieldRenderer;
}

export interface FieldTypeDescription {
    class?: string;
    args?: any;

    style?: SlStyle;
    textType?: string;
}
