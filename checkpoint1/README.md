hello
instructions and other stuffs

## Phase 2 build (JFlex + CUP)

1. **JFlex**: Install JFlex and ensure `jflex` is on your PATH (e.g. from [JFlex releases](https://github.com/jflex-de/jflex/releases)).
2. **CUP**: Download `java_cup.jar` (e.g. from [CUP project](https://www2.cs.tum.edu/projects/cup/)) and place it in `lib/java_cup.jar`.
3. Run:
   ```
   make
   ```
4. Run the compiler on a valid C- file:
   ```
   java CM tests/1.cm
   java CM -a tests/1.cm
   ```