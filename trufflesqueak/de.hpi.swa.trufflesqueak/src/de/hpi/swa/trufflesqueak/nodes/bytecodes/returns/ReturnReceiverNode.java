package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;

public class ReturnReceiverNode extends ReturnNode {
    @Child private ReceiverNode receiverNode;

    public ReturnReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
        receiverNode = new ReceiverNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new LocalReturn(receiverNode.execute(frame));
    }

    @Override
    public String toString() {
        return "returnSelf";
    }
}
