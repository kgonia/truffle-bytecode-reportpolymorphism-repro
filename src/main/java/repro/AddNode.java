package repro;

import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

/**
 * An ordinary AST node that reports polymorphism (as an AST interpreter's nodes do for splitting), made proxyable
 * so a Bytecode DSL interpreter can reuse it as an operation.
 */
@ReportPolymorphism
@OperationProxy.Proxyable
public abstract class AddNode extends Node {
    public abstract Object execute(Object left, Object right);

    @Specialization
    public static long addLongs(long left, long right) {
        return left + right;
    }

    @Specialization
    public static double addDoubles(double left, double right) {
        return left + right;
    }

    @Specialization
    public static String concat(String left, String right) {
        return left + right;
    }
}
