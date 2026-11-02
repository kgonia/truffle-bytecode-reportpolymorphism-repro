package repro;

import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;

/** A minimal Bytecode DSL interpreter whose only operation proxies {@link AddNode}. */
@GenerateBytecode(languageClass = ReproLanguage.class)
@OperationProxy(AddNode.class)
public abstract class ReproRootNode extends RootNode implements BytecodeRootNode {
    protected ReproRootNode(ReproLanguage language, FrameDescriptor frameDescriptor) {
        super(language, frameDescriptor);
    }
}
