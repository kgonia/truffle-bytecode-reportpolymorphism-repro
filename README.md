# Bytecode DSL: an operation with `@ReportPolymorphism` doesn't compile (Truffle 25.1+)

Since Truffle 25.1, a Bytecode DSL operation that reports polymorphism makes the DSL generate a
`checkForPolymorphicSpecialize` method that reads the specialization state from the bytecode array (`$bc`, `$bci`)
without having them in scope. javac rejects the generated interpreter. This happens both for a regular `@Operation`
and for an AST node reused through `@OperationProxy`. Truffle 25.0 compiles both.

## The reproducer

- Case 1, [`ReproRootNode`](src/main/java/repro/ReproRootNode.java): `@GenerateBytecode` with one `@Operation`
  annotated `@ReportPolymorphism` (three static specializations).
- Case 2, [`ProxyRootNode`](src/main/java/repro/ProxyRootNode.java): `@GenerateBytecode` with
  `@OperationProxy(AddNode.class)`, where [`AddNode`](src/main/java/repro/AddNode.java) is an ordinary AST node
  annotated `@ReportPolymorphism @OperationProxy.Proxyable` (the usual shape when migrating an AST interpreter whose
  nodes report polymorphism for splitting).
- [`ReproLanguage`](src/main/java/repro/ReproLanguage.java): an empty language for `languageClass`.

```sh
mvn clean compile                       # Truffle 25.4.4.1.1
./check-versions.sh                     # 25.0.4, 25.1.3, 25.2.4, 25.3.4.1, 25.4.4.1.1
```

| Truffle | result |
|---|---|
| 25.0.4 | compiles |
| 25.1.3 | fails, both cases |
| 25.2.4 | fails, both cases |
| 25.3.4.1 | fails, both cases |
| 25.4.4.1.1 (latest release) | fails, both cases |
| 25.6.0-dev snapshot ([25.6.0-dev-20261102_0204](https://github.com/graalvm/graalvm-ce-dev-builds/releases/tag/25.6.0-dev-20261102_0204), `truffle-dsl-processor-25.6.0-20261102.011351-1`, compiled with javac) | fails, both cases |

## Error (25.4.4.1.1)

```
ReproRootNodeGen.java: cannot find symbol
  symbol:   variable $bc
  location: class repro.ReproRootNodeGen.Add_Node
ReproRootNodeGen.java: cannot find symbol
  symbol:   variable $bci
  location: class repro.ReproRootNodeGen.Add_Node
(the same two errors for repro.ProxyRootNodeGen.Add_Node)
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

## Where it comes from

[GR-61293](https://github.com/oracle/graal/pull/12270) ("Bytecode-DSL: Inline state bits into the bytecode array
whenever possible", commit `56e3cc4301`, merged 2025-10-02, Truffle 25.1; CHANGELOG: "Specialization state is now
inlined into the bytecode array"):

- `InstructionModel.canInlineState()` inlines the state of every operation except those with inlined-node state
  fields. Polymorphism reporting isn't considered.
- `executeAndSpecialize` gets `$bc` and `$bci` as parameters and calls `checkForPolymorphicSpecialize(oldState_0)`
  from its `finally`.
- `FlatNodeGenFactory.createCheckForPolymorphicSpecialize` (unchanged by that PR) loads the new state with
  `multiState.createLoad(frameState, ...)` on a fresh slow-path `FrameState` that has no bytecode parameters. With the
  state in a node field (25.0) that load needed nothing more; with the state in the bytecode array it reads `$bc`
  and `$bci`, which the method doesn't have.

It doesn't look intentional: the PR and the CHANGELOG don't mention polymorphism reporting, the DSL doesn't reject or
warn about `@ReportPolymorphism` on an operation, and none of the Bytecode DSL tests
(`com.oracle.truffle.api.bytecode.test`, 80 files on master) uses `@ReportPolymorphism`, so the combination is
untested. Graal master's `canInlineState()` and `createCheckForPolymorphicSpecialize` are still as above, and the
2026-11-02 snapshot fails the same way.

## Expected

`@ReportPolymorphism` on an operation compiles and reports polymorphism, as on 25.0. Two possible fixes:

- pass `$bc`/`$bci` (or the new state) to `checkForPolymorphicSpecialize` when the state is inlined, or
- have `canInlineState()` return false for nodes that report polymorphism, keeping their state in a field as before.

If reporting polymorphism from bytecode operations isn't meant to be supported, a DSL error or warning instead of
uncompilable generated code.

## Workaround

Exclude the operation from polymorphism reporting. For a proxied node, proxy a subclass:

```java
@OperationProxy.Proxyable
@ReportPolymorphism.Exclude
public abstract class AddOperation extends AddNode {
}
```

This compiles on 25.0.4 and 25.4.4.1.1, but the operation then never reports polymorphism, so it never causes its
bytecode root to be split.

Found while porting [Truffle PHP](https://github.com/kgonia/graalphp) to the Bytecode DSL, whose AST nodes report
polymorphism for splitting.
