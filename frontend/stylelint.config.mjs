export default {
  extends: ['stylelint-config-standard'],
  rules: {
    // Existing game class names and cascade are intentional, not formatting errors.
    'selector-class-pattern': null,
    'keyframes-name-pattern': null,
    'no-descending-specificity': null,
    // Prettier owns representation; these are valid equivalent CSS spellings.
    'color-function-notation': null,
    'alpha-value-notation': null,
    'color-hex-length': null,
    'rule-empty-line-before': null,
    'at-rule-empty-line-before': null,
    'comment-empty-line-before': null,
    'declaration-empty-line-before': null,
    'custom-property-empty-line-before': null,
    'declaration-block-single-line-max-declarations': null,
    'media-feature-range-notation': null,
  },
};
