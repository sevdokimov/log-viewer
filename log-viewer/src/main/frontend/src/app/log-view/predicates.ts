import {SearchPattern} from './search';

export interface Predicate {
    type: string;
}

export interface GroovyPredicate extends Predicate {
    script: string;
}

export interface SubstringPredicate extends Predicate {
    search: SearchPattern;
}

export interface CompositeRecordPredicate extends Predicate {
    predicates: Predicate[];
    isAnd: boolean;
}

export interface NotPredicate extends Predicate {
    delegate: Predicate;
}

export interface FieldValueSetPredicate extends Predicate {
    fieldType: string;
    values: string[];
}

export interface ExceptionOnlyPredicate extends Predicate {

}
