package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.context.UpdateInstructionPointerNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public final class ExecuteContextNode extends AbstractNodeWithCode {
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleLocalReturnNode handleLocalReturnNode;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private HandleNonVirtualReturnNode handleNonVirtualReturnNode = HandleNonVirtualReturnNode.create();
    @Child private UpdateInstructionPointerNode updateInstructionPointerNode;
    @Child private StackPushNode pushStackNode = StackPushNode.create();
    @Child private CalculcatePCOffsetNode calculcatePCOffsetNode = CalculcatePCOffsetNode.create();
    @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    public static ExecuteContextNode create(final CompiledCodeObject code) {
        return new ExecuteContextNode(code);
    }

    protected ExecuteContextNode(final CompiledCodeObject code) {
        super(code);
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        handleLocalReturnNode = HandleLocalReturnNode.create(code);
        handleNonLocalReturnNode = HandleNonLocalReturnNode.create(code);
        updateInstructionPointerNode = UpdateInstructionPointerNode.create(code);
    }

    public Object executeVirtualized(final VirtualFrame frame) {
        try {
            if (!code.hasPrimitive() && bytecodeNodes.length > 32) {
                code.image.interrupt.sendOrBackwardJumpTrigger(frame);
            }
            startBytecode(frame);
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            getOrCreateContextNode.executeGet(frame).markEscaped();
            return handleNonVirtualReturnNode.executeHandle(frame, nvr);
        } catch (ProcessSwitch ps) {
            getOrCreateContextNode.executeGet(frame).markEscaped();
            throw ps;
        } finally {
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    public Object executeNonVirtualized(final VirtualFrame frame, final ContextObject newContext) {
        // maybe persist newContext, so there's no need to lookup the context to update its pc.
        assert newContext.getClosureOrMethod() == frame.getArguments()[FrameAccess.METHOD];

        try {
            if (!code.hasPrimitive() && bytecodeNodes.length > 32) {
                code.image.interrupt.sendOrBackwardJumpTrigger(frame);
            }
            final long initialPC = getAndDecodeSqueakPC(newContext);
            if (initialPC == 0) {
                startBytecode(frame);
            } else {
                // avoid optimizing the cases in which a context is resumed
                CompilerDirectives.transferToInterpreter();
                resumeBytecode(frame, initialPC);
            }
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            return handleNonVirtualReturnNode.executeHandle(frame, nvr);
        } finally {
            MaterializeContextOnMethodExitNode.stopMaterializationHere();
        }
    }

    private long getAndDecodeSqueakPC(final ContextObject newContext) {
        return newContext.getInstructionPointer() - calculcatePCOffsetNode.execute(newContext.getClosureOrMethod());
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void startBytecode(final VirtualFrame frame) {
        int pc = 0;
        int backJumpCounter = 0;
        AbstractBytecodeNode node = bytecodeNodes[pc];
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                updateInstructionPointerNode.executeUpdate(frame, node.getSuccessorIndex());
                if (node instanceof ConditionalJumpNode) {
                    final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    if (jumpNode.executeCondition(frame)) {
                        final int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    } else {
                        final int successor = jumpNode.getSuccessorIndex();
                        if (CompilerDirectives.inInterpreter()) {
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    node = bytecodeNodes[pc];
                    continue;
                } else {
                    try {
                        pc = node.executeInt(frame);
                    } catch (NonLocalReturn nlr) {
                        if (nlr.hasArrivedAtTargetContext()) {
                            pushStackNode.executeWrite(frame, nlr.getReturnValue());
                            pc = node.getSuccessorIndex();
                        } else {
                            throw nlr;
                        }
                    }
                    node = bytecodeNodes[pc];
                    continue;
                }
            }
        } finally {
            assert backJumpCounter >= 0;
            LoopNode.reportLoopCount(this, backJumpCounter);
        }
    }

    /*
     * Non-optimized version of startBytecode which is used to resume contexts.
     */
    protected void resumeBytecode(final VirtualFrame frame, final long initialPC) {
        int pc = (int) initialPC;
        AbstractBytecodeNode node = bytecodeNodes[pc];
        while (pc >= 0) {
            updateInstructionPointerNode.executeUpdate(frame, node.getSuccessorIndex());
            try {
                pc = node.executeInt(frame);
            } catch (NonLocalReturn nlr) {
                if (nlr.hasArrivedAtTargetContext()) {
                    pushStackNode.executeWrite(frame, nlr.getReturnValue());
                    pc = node.getSuccessorIndex();
                } else {
                    throw nlr;
                }
            }
            node = bytecodeNodes[pc];
        }
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return code.toString();
    }
}
