import java.io.*;
import java.util.Hashtable;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class VMWriter {

    private static Hashtable<String, String> segments;
    private BufferedWriter writer;

    static {
        segments = new Hashtable<String, String>();
        segments.put(Segment.CONST.toString(), "constant");
        segments.put(Segment.ARG.toString(), "argument");
        segments.put(Segment.LOCAL.toString(), "local");
        segments.put(Segment.STATIC.toString(), "static");
        segments.put(Segment.THIS.toString(), "this");
        segments.put(Segment.THAT.toString(), "that");
        segments.put(Segment.POINTER.toString(), "pointer");
        segments.put(Segment.TEMP.toString(), "temp");
    }

    VMWriter(Path path) throws Exception {
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }

    void writePush(Segment segment, int index) throws Exception {
        final String command = String.format("push %s %d", segments.get(segment.toString()), index);
        writer.write(command, 0, command.length());
        writer.newLine();
    }

    void writePop(Segment segment, int index) throws Exception {
        final String command = String.format("pop %s %d", segments.get(segment.toString()), index);
        writer.write(command, 0, command.length());
        writer.newLine();
    }

    void writeArithmetic(Command command) throws Exception {
        writer.write(command.toString().toLowerCase(), 0, command.toString().length());
        writer.newLine();
    }

    void writeLabel(String label) throws Exception {
        final String command = String.format("label %s", label);
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }
    
    void writeGoto(String label) throws Exception {
        final String command = String.format("goto %s", label);
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }

    void writeIf(String label) throws Exception {
        final String command = String.format("if-goto %s", label);
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }

    void writeCall(String functionName, int nArgs) throws Exception {
        final String command = String.format("call %s %d", functionName, nArgs);
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }

    void writeFunction(String functionName, int nLocals) throws Exception {
        final String command = String.format("function %s %d", functionName, nLocals);
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }

    void writeReturn() throws Exception {
        final String command = "return";
        writer.write(command.toString(), 0, command.toString().length());
        writer.newLine();
    }

    void close() throws Exception {
        if (writer != null) { writer.close(); }
    }
}