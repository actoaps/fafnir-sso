const path = require('path');

//  To do
//  1. load styles - possibly compile sass with certain parameters
// 1.1 e.g., autoprefixing and minification
// 2. generate the templates? 


module.exports = {
    entry: './src/index.js',
    output: {
        filename: 'main.js',
        path: path.resolve(__dirname, 'dist'),
    },
};