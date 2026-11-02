package repro;

import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.OperationProxy;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;

/** Case 2: an existing AST node that reports polymorphism, reused through {@link OperationProxy}. */
@GenerateBytecode(languageClass = ReproLanguage.class)
@OperationProxy(AddNode.class)
public abstract class ProxyRootNode extends RootNode implements BytecodeRootNode {
    protected ProxyRootNode(ReproLanguage language, FrameDescriptor frameDescriptor) {
        super(language, frameDescriptor);
    }
}
