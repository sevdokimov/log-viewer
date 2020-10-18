/*
 * Extra typings definitions
 */

// Allow .json files imports
declare module '*.json';

// SystemJS module definition
declare var module: NodeModule;

interface NodeModule {
    id: string;
}

declare namespace Reflect {
    function defineMetadata(metadataKey: any, metadataValue: any, target: Object, propertyKey: string | symbol): void;
    function getMetadata(metadataKey: any, target: Object, propertyKey: string | symbol): any;
}
