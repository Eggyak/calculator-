Minimal desktop calculator UI with visible calculation history.

The default launcher is the CLI. Type `gui` to open the desktop interface, or run
`java Main gui` directly. Supported operations include:

You can enter a complete expression directly, for example `4/7`, `2+3*4`,
`(8-2)/3`, `sqrt(9)`, or `sin(30)`.

- Binary: `+`, `-`, `*`, `/`, `%`, `^`
- Trigonometry in degrees: `sin`, `cos`, `tan`, `asin`, `acos`, `atan`
- Scientific: `sqrt`, `cbrt`, `square`, `reciprocal`, `log`, `ln`, `abs`, `exp`, `fact`
- Constants: `pi`, `e`

Angles for `sin` and `cos` are entered in degrees. Compile and run with:

```bash
javac *.java && java Main
```
