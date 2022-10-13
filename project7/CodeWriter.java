import java.io.*;
import java.util.Hashtable;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class CodeWriter {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // Arithmetic/Logical commands
    private static final String CMD_ADD = "// add"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    M=D+M"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_SUB = "// sub"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    M=M-D"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_NEG = "// neg"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=-M"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_EQ = "// eq"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    D=M-D"
        + LINE_SEPARATOR + "    @EQ_[index]"
        + LINE_SEPARATOR + "    D;JEQ"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=0"
        + LINE_SEPARATOR + "    @NEQ_[index]"
        + LINE_SEPARATOR + "    0;JMP"
        + LINE_SEPARATOR + "(EQ_[index])"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=-1"
        + LINE_SEPARATOR + "(NEQ_[index])"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_GT = "// gt"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    D=M-D"
        + LINE_SEPARATOR + "    @GT_[index]"
        + LINE_SEPARATOR + "    D;JGT"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=0"
        + LINE_SEPARATOR + "    @NGT_[index]"
        + LINE_SEPARATOR + "    0;JMP"
        + LINE_SEPARATOR + "(GT_[index])"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=-1"
        + LINE_SEPARATOR + "(NGT_[index])"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_LT = "// lt"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    D=M-D"
        + LINE_SEPARATOR + "    @LT_[index]"
        + LINE_SEPARATOR + "    D;JLT"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=0"
        + LINE_SEPARATOR + "    @NLT_[index]"
        + LINE_SEPARATOR + "    0;JMP"
        + LINE_SEPARATOR + "(LT_[index])"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=-1"
        + LINE_SEPARATOR + "(NLT_[index])"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_AND = "// and"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    M=D&M"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_OR = "// or"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    A=A-1"
        + LINE_SEPARATOR + "    M=D|M"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_NOT = "// not"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M-1"
        + LINE_SEPARATOR + "    M=!M"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;

    // Memory Access commands
    private static final String CMD_PUSH_CONSTANT = "// push constant [i]"
        + LINE_SEPARATOR + "    @[i]"
        + LINE_SEPARATOR + "    D=A"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    M=M+1"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_PUSH_SEGLATT = "// push [segment] [i]"
        + LINE_SEPARATOR + "    @[i]"
        + LINE_SEPARATOR + "    D=A"
        + LINE_SEPARATOR + "    @[segment]"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    A=D+A"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    M=M+1"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_POP_SEGLATT = "// pop [segment] [i]"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @[i]"
        + LINE_SEPARATOR + "    D=D+A"
        + LINE_SEPARATOR + "    @[segment]"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    D=D+A"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    A=D-M"
        + LINE_SEPARATOR + "    M=D-A"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_PUSH_TEMP = "// push temp [i]"
        + LINE_SEPARATOR + "    @[i]"
        + LINE_SEPARATOR + "    D=A"
        + LINE_SEPARATOR + "    @R5"
        + LINE_SEPARATOR + "    A=D+A"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    M=M+1"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_POP_TEMP = "// pop temp [i]"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @[i]"
        + LINE_SEPARATOR + "    D=D+A"
        + LINE_SEPARATOR + "    @R5"
        + LINE_SEPARATOR + "    D=D+A"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    A=D-M"
        + LINE_SEPARATOR + "    M=D-A"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_PUSH_POINTER = "// push pointer [i]"
        + LINE_SEPARATOR + "    @[pointer]"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    M=M+1"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_POP_POINTER = "// pop pointer [i]"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @[pointer]"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_PUSH_STATIC = "// push static [i]"
        + LINE_SEPARATOR + "    @[prefix].[i]"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    A=M"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    M=M+1"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;
    private static final String CMD_POP_STATIC = "// pop static [i]"
        + LINE_SEPARATOR + "    @SP"
        + LINE_SEPARATOR + "    AM=M-1"
        + LINE_SEPARATOR + "    D=M"
        + LINE_SEPARATOR + "    @[prefix].[i]"
        + LINE_SEPARATOR + "    M=D"
        + LINE_SEPARATOR
        + LINE_SEPARATOR;

    private static Hashtable<String, String> arithLogicCommands;
    private static Hashtable<String, String> segmentSymbols;
    private static String[] pointers;
    static {
        arithLogicCommands = new Hashtable<String, String>();
        segmentSymbols = new Hashtable<String, String>();
        pointers = new String[2];

        arithLogicCommands.put("add", CMD_ADD);
        arithLogicCommands.put("sub", CMD_SUB);
        arithLogicCommands.put("neg", CMD_NEG);
        arithLogicCommands.put("eq", CMD_EQ);
        arithLogicCommands.put("gt", CMD_GT);
        arithLogicCommands.put("lt", CMD_LT);
        arithLogicCommands.put("and", CMD_AND);
        arithLogicCommands.put("or", CMD_OR);
        arithLogicCommands.put("not", CMD_NOT);

        segmentSymbols.put("local", "LCL");
        segmentSymbols.put("argument", "ARG");
        segmentSymbols.put("this", "THIS");
        segmentSymbols.put("that", "THAT");

        pointers[0] = "THIS";
        pointers[1] = "THAT";
    }

    private File file;
    private BufferedWriter writer;
    private long eqIndex;
    private long gtIndex;
    private long ltIndex;
    private String staticPrefix;

    CodeWriter(String pathname) throws Exception {
        file = new File(pathname);
        eqIndex = 0;
        gtIndex = 0;
        ltIndex = 0;
        staticPrefix = file.getName().substring(0, file.getName().lastIndexOf('.'));
        writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    }

    void writeArithmetic(String command) throws Exception {
        String cmd = arithLogicCommands.get(command);

        // Insert unique index for labels to avoid name conflicts
        switch (command) {
            case "eq":
                cmd = cmd.replace("[index]", Long.toString(eqIndex));
                eqIndex++;
                break;
            case "gt":
                cmd = cmd.replace("[index]", Long.toString(gtIndex));
                gtIndex++;
                break;
            case "lt":
                cmd = cmd.replace("[index]", Long.toString(ltIndex));
                ltIndex++;
                break;
        }
        
        writer.write(cmd, 0, cmd.length());
    }

    void writePushPop(CommandType commandType, String segment, int index) throws Exception {
        String cmd = null;

        switch (segment) {
            case "constant":
                cmd = CMD_PUSH_CONSTANT.replace("[i]", Integer.toString(index));
                break;
            case "local":
            case "argument":
            case "this":
            case "that":
                cmd = commandType == CommandType.C_PUSH ? CMD_PUSH_SEGLATT : CMD_POP_SEGLATT;
                // First replace comment then insert symbols
                cmd = cmd.replaceFirst("\\[segment\\]", segment).replace("[segment]", segmentSymbols.get(segment)).replace("[i]", Integer.toString(index));
                break;
            case "static":
                cmd = commandType == CommandType.C_PUSH ? CMD_PUSH_STATIC : CMD_POP_STATIC;
                cmd = cmd.replace("[prefix]", staticPrefix).replace("[i]", Integer.toString(index));
                break;
            case "temp":
                cmd = commandType == CommandType.C_PUSH ? CMD_PUSH_TEMP : CMD_POP_TEMP;
                cmd = cmd.replace("[i]", Integer.toString(index));
                break;
            case "pointer":
                cmd = commandType == CommandType.C_PUSH ? CMD_PUSH_POINTER : CMD_POP_POINTER;
                cmd = cmd.replace("[pointer]", pointers[index]).replace("[i]", Integer.toString(index));
                break;
        }
        
        writer.write(cmd, 0, cmd.length());
    }

    void close() throws Exception {
        if (writer != null) 
            writer.close();
    }
}