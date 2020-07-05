import {FieldRenderer} from './renderer';
import {SlElement} from '@app/utils/sl-element';
import {Record} from '@app/log-view/record';

export class ClassNameFieldRenderer implements FieldRenderer {
    private static classNameRegex: RegExp = new RegExp(
        /^((?:[a-zA-Z_\$][a-zA-Z_\$0-9]*\.)+)([a-zA-Z_\$][a-zA-Z_\$0-9]*)$/
    );

    constructor(args: any) {
    }

    append(e: HTMLElement, s: string, record: Record): void {
        let res = ClassNameFieldRenderer.classNameRegex.exec(s);
        if (!res) {
            e.append(s);
            return;
        }

        let pkg = res[1];

        if (pkg.length <= 4) { return null; }

        let wrapper = document.createElement('SPAN');
        wrapper.className = 'coll-wrapper collapsed';
        wrapper.title = s;

        let expander = document.createElement('SPAN');
        expander.className = 'coll-expander cls-package-expander';
        expander.innerText = '~.';
        (<SlElement>expander).virtual = true;

        wrapper.appendChild(expander);

        let expandedPackage = document.createElement('SPAN');
        expandedPackage.className = 'coll-body-span';
        expandedPackage.innerText = pkg;

        wrapper.appendChild(expandedPackage);

        wrapper.appendChild(document.createTextNode(res[2]));

        e.append(wrapper);
    }
}
