package de.hpi.swa.trufflesqueak.util;

public final class Constants {
    public static final class SPECIAL_OBJECT_INDEX {
        public static final int NilObject = 0;
        public static final int FalseObject = 1;
        public static final int TrueObject = 2;
        public static final int SchedulerAssociation = 3;
        public static final int ClassBitmap = 4;
        public static final int ClassInteger = 5;
        public static final int ClassString = 6;
        public static final int ClassArray = 7;
        public static final int SmalltalkDictionary = 8;
        public static final int ClassFloat = 9;
        public static final int ClassMethodContext = 10;
        public static final int ClassBlockContext = 11;
        public static final int ClassPoint = 12;
        public static final int ClassLargePositiveInteger = 13;
        public static final int TheDisplay = 14;
        public static final int ClassMessage = 15;
        public static final int ClassCompiledMethod = 16;
        public static final int TheLowSpaceSemaphore = 17;
        public static final int ClassSemaphore = 18;
        public static final int ClassCharacter = 19;
        public static final int SelectorDoesNotUnderstand = 20;
        public static final int SelectorCannotReturn = 21;
        public static final int TheInputSemaphore = 22;
        public static final int SpecialSelectors = 23;
        public static final int CharacterTable = 24;
        public static final int SelectorMustBeBoolean = 25;
        public static final int ClassByteArray = 26;
        public static final int ClassProcess = 27;
        public static final int CompactClasses = 28;
        public static final int TheTimerSemaphore = 29;
        public static final int TheInterruptSemaphore = 30;
        public static final int FloatProto = 31;
        public static final int SelectorCannotInterpret = 34;
        public static final int MethodContextProto = 35;
        public static final int ClassBlockClosure = 36;
        public static final int BlockContextProto = 37;
        public static final int ExternalObjectsArray = 38;
        public static final int ClassPseudoContext = 39;
        public static final int ClassTranslatedMethod = 40;
        public static final int TheFinalizationSemaphore = 41;
        public static final int ClassLargeNegativeInteger = 42;
        public static final int ClassExternalAddress = 43;
        public static final int ClassExternalStructure = 44;
        public static final int ClassExternalData = 45;
        public static final int ClassExternalFunction = 46;
        public static final int ClassExternalLibrary = 47;
        public static final int SelectorAboutToReturn = 48;
        public static final int SelectorRunWithIn = 49;
        public static final int SelectorAttemptToAssign = 50;
        public static final int PrimErrTableIndex = 51;
        public static final int ClassAlien = 52;
        public static final int InvokeCallbackSelector = 53;
        public static final int ClassUnsafeAlien = 54;
        public static final int ClassWeakFinalizer = 55;
    }

    public static final class MOUSE_BUTTON {
        public static final int ALL = 1 + 2 + 4;
        public static final int BLUE = 1;
        public static final int YELLOW = 2;
        public static final int RED = 4;
    }

    public static final class KEYBOARD_MODIFIER {
        public static final int ALL = 8 + 16 + 32 + 64;
        public static final int SHIFT = 8;
        public static final int CTRL = 16;
        public static final int ALT = 32;
        public static final int CMD = 64;
    }
}
