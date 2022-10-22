# Vector

This is an RISC-V Vector RTL generator.
It can be configured with RISC-V Vector Zvl1024b,Zve32x extension for now.  
It can only work with a standard RISC-V core.
Currently, only Rocket is supported, which will be maintained in another project.
In this project, only the RISC-V Vector coprocessor is maintained.
The CI is an unittest to RTL, using spike as emulator to serve as a signal driver and checker.

## Build

Vector use [mill](https://github.com/com-lihaoyi/mill) as build system.

Generate Verilog with `mill vector.elaborate.rtls`, RTL will be generated in `out/vector/elaborate/rtls`.  
The Scala code in `elaborate` project use AOP to modify RTLs for testing, performance evaluation and tapeout elaboration.  

The cosim framework can be generated with `mill vector.elaborate.verilated`.
The output emulator can run binary, generate the waveform, and cosim with [spike](https://github.com/riscv/riscv-isa-sim).

## Patches
<!-- BEGIN-PATCH -->
musl https://github.com/sequencer/musl/compare/master...riscv32.diff
chisel3 https://github.com/chipsalliance/chisel3/compare/master...circt_aop.diff
<!-- END-PATCH -->
