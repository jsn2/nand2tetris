// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

(REFRESH)
    @SCREEN
    D=A
    @addr
    M=D

    @colour
    M=0

    // Set key = KBD[0]
    @KBD
    D=M
    @key
    M=D

    // if key == 0 goto LOOP
    @colour
    D=D-M
    @LOOP
    D;JEQ

    @colour
    M=-1

(LOOP)
    // if addr == KBD goto CHECK
    @addr
    D=M
    @KBD
    D=A-D
    @CHECK
    D;JEQ

    // Set addr[0] = colour
    @colour
    D=M
    @addr
    A=M
    M=D

    //Increment addr
    @addr
    M=M+1

    @LOOP
    0;JMP

(CHECK)
    // if KBD[0] == key goto CHECK
    @KBD
    D=M
    @key
    D=M-D
    @CHECK
    D;JEQ

    @REFRESH
    0;JMP