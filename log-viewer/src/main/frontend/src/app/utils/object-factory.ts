import {JavaExceptionRenderer} from '@app/log-view/renderers/java-exception-renderer';
import {ClassNameFieldRenderer} from '@app/log-view/renderers/class-name-field-renderer';
import {FixedTextRenderer} from '@app/log-view/renderers/fixed-text-renderer';
import {FixedTextFieldRenderer} from '@app/log-view/renderers/fixed-text-field-renderer';
import {TextFieldRenderer} from '@app/log-view/renderers/text-field-renderer';
import {DateFieldRenderer} from '@app/log-view/renderers/date-field-renderer';
import {MillisecondRenderer} from '@app/log-view/renderers/millisecond-renderer';
import {DateInMillisecondRenderer} from '@app/log-view/renderers/date-in-millisecond-renderer';
import {BracketsHighlighter} from '@app/log-view/renderers/brackets-highlighter';
import {RegexHighlighter} from '@app/log-view/renderers/regex-highlighter';

export class ObjectFactory {
    static createRenderer(rendererType: string, args: any): any {
        switch (rendererType) {
            case 'JavaExceptionRenderer':
                return new JavaExceptionRenderer(args);
            case 'MillisecondRenderer':
                return new MillisecondRenderer(args);
            case 'DateInMillisecondRenderer':
                return new DateInMillisecondRenderer(args);
            case 'BracketsHighlighter':
                return new BracketsHighlighter(args);
            case 'RegexHighlighter':
                return new RegexHighlighter(args);

            // Field renderers

            case 'ClassNameFieldRenderer':
                return new ClassNameFieldRenderer(args);
            case 'FixedTextRenderer':
                return new FixedTextRenderer(args);
            case 'FixedTextFieldRenderer':
                return new FixedTextFieldRenderer(args);
            case 'DateFieldRenderer':
                return new DateFieldRenderer(args);

            case 'TextFieldRenderer':
                return new TextFieldRenderer(args);

            default:
                throw 'Unknown class type: ' + rendererType;
        }
    }
}
