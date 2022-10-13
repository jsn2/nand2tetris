// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

    // Set sum to 0
    @0
    D=A
    @sum
    M=D

    // Set i to 1
    @1
    D=A
    @i
    M=D

(LOOP)
    // Stop if i < R1
    @i
    D=M
    @R1
    D=D-M;
    @STOP
    D;JGT

    // Add R0 to sum and increment i
    @R0
    D=M
    @sum
    M=D+M

    @i
    M=M+1

    @LOOP
    0;JMP

(STOP)
    // Set R2 to sum
    @sum
    D=M
    @R2
    M=D

(END)
    @END
    0;JMP