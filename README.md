# README
A reference implementation for Prof. Elaine Shi's Circuit Oblivious Heap algorithm, on top of wangxiao1254's garbled circuit implementation (https://github.com/wangxiao1254/FlexSC).

# Requirements

Known to compile with OpenJDK 11.0.4.

# Files

Significant changes from wangxiao1254's Circuit ORAM implementation include:
- `src/oheap/CircuitOHeap`: the primary file, containing reference implementations for all six operations (findMin, delete, insert, extractMin, decreaseKey, increaseKey) as well as an option for enabling type-hiding security.
- `src/oheap/Block` and `PlainBlock`: now contain a key field in addition to the previous t/iden, leafId/pos, data, and isDummy fields. Corresponding changes in other files have been made to e.g. serialization to account for the extra field.
- `src/oheap/TreeBasedOHeapParty`: now handles securely updating subtree mins.
- `test/oheap/CountCircuitOHeap`: Runs the Circuit OHeap with an env set to `Mode.COUNT`, which quickly counts circuit statistics in place of running the full protocol.
- `test/oheap/TestCircuitOHeapVerify`: Runs the Circuit OHeap with an env set to `Mode.VERIFY` alongside the same operations done on a regular heap to verify the correctness of the implementation.
- `test/oheap/TestCircuitOHeapReal`: Similar to above, but runs the Circuit OHeap with an env set to `Mode.REAL` instead. This test may run slower than the previous two tests.

For more information on CompEnv modes, see https://github.com/wangxiao1254/FlexSC/blob/master/README.md.

# Compiling and Running

Compile with `./compile.sh` and run with any of `./runCountOHeap.sh`, `./runTestOHeapVerify.sh`, or `./runTestOHeapReal.sh`.
