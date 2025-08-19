// <reference types="webpack-env" />
import './main.scss';

// Import all JS/TS files from site folder
const context = require.context('./', true, /\.(js|ts)$/);
context.keys().forEach(context);

// Import all JS/JSX files from components
const componentsContext = require.context('../components', true, /\.(js|jsx)$/);
componentsContext.keys().forEach(componentsContext);
