package repro;

import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;

/** Case 1: a regular operation that reports polymorphism. */
@GenerateBytecode(languageClass = ReproLanguage.class)
public abstract class ReproRootNode extends RootNode implements BytecodeRootNode {
    protected ReproRootNode(ReproLanguage language, FrameDescriptor frameDescriptor) {
        super(language, frameDescriptor);
    }

    @Operation
    @ReportPolymorphism
    public static final class Add {
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
}
