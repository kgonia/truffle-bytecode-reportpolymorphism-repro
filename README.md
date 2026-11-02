# Bytecode DSL: `@OperationProxy` of a `@ReportPolymorphism` node doesn't compile (Truffle 25.1+)

A node annotated with `@ReportPolymorphism` and `@OperationProxy.Proxyable`, used as an operation through
`@OperationProxy`, makes the Bytecode DSL generate a `checkForPolymorphicSpecialize` method that reads the
specialization state from the bytecode array (`$bc`, `$bci`) without having them in scope. javac rejects the
generated interpreter.

The whole reproducer is three classes:

- [`AddNode`](src/main/java/repro/AddNode.java): `@ReportPolymorphism @OperationProxy.Proxyable`, three static
  specializations.
- [`ReproRootNode`](src/main/java/repro/ReproRootNode.java): `@GenerateBytecode` with `@OperationProxy(AddNode.class)`
  as its only operation.
- [`ReproLanguage`](src/main/java/repro/ReproLanguage.java): an empty language for `languageClass`.

## Reproduce

```sh
mvn clean compile                       # Truffle 25.4.4.1.1
./check-versions.sh                     # 25.0.4, 25.1.3, 25.2.4, 25.3.4.1, 25.4.4.1.1
```

| Truffle | result |
|---|---|
| 25.0.4 | compiles |
| 25.1.3 | fails |
| 25.2.4 | fails |
| 25.3.4.1 | fails |
| 25.4.4.1.1 | fails |

## Error (25.4.4.1.1)

```
ReproRootNodeGen.java:[10453,62] cannot find symbol
  symbol:   variable $bc
  location: class repro.ReproRootNodeGen.Add_Node
ReproRootNodeGen.java:[10453,67] cannot find symbol
  symbol:   variable $bci
  location: class repro.ReproRootNodeGen.Add_Node
```

The generated code:

```java
        private void checkForPolymorphicSpecialize(int oldState_0) {
            int state_0 = Short.toUnsignedInt(BYTES.getShort($bc, $bci + 2 /* imm state_0 */));
            int newState_0 = state_0;
            if (((oldState_0 ^ newState_0) != 0)) {
                this.reportPolymorphicSpecialize();
            }
        }
```

`executeAndSpecialize` has `$bc` and `$bci` as parameters and calls `checkForPolymorphicSpecialize(oldState_0)` from
its `finally`. The check loads the state again (`multiState.createLoad(frameState, ...)` in
`FlatNodeGenFactory.createCheckForPolymorphicSpecialize`), but its `FrameState` is a fresh slow-path one with no
bytecode parameters. Since 25.1 the state of a proxied node lives in the bytecode array instead of a field, so the
load needs them. On 25.0 the state was a field, so the same code compiled. Graal master's
`createCheckForPolymorphicSpecialize` looks unchanged; I haven't built master.

## Expected

Either the generated code passes `$bc`/`$bci` (or the new state) to `checkForPolymorphicSpecialize`, or the DSL
reports a clear error/warning that `@ReportPolymorphism` isn't supported on proxied operations.

## Workaround

A subclass of the node with `@ReportPolymorphism.Exclude` as the proxied class. It compiles, but the operation never
reports polymorphism, so a bytecode root is never split because of it.

```java
@OperationProxy.Proxyable
@ReportPolymorphism.Exclude
public abstract static class AddOperation extends AddNode {
}
```

Found while porting [Truffle PHP](https://github.com/kgonia/graalphp) to the Bytecode DSL, whose AST nodes report
polymorphism for splitting.
