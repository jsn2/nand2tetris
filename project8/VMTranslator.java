import java.io.*;

class VMTranslator {
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator filename");
            System.exit(1);
        }

        // Initialise to null to avoid compiler warnings
        Parser parser = null;
        CodeWriter codeWriter = null;

        try {
            File pathname = new File(args[0]).getCanonicalFile();

            // Check file or directory exists
            if (!pathname.exists() || !(pathname.isFile() || pathname.isDirectory()))
                throw new Exception("File or directory '" + args[0] + "' not found.");
            
            // Get vm files to translate and set output file pathname
            File[] files;
            String outputPathname;
            boolean includeStartupCode = true;
            if (pathname.isFile()) {
                // Check file is valid
                if (pathname.getName().endsWith(".vm")) {
                    files = new File[1];
                    files[0] = new File(pathname.getPath());
                    
                    outputPathname = pathname.getPath().substring(0, pathname.getPath().lastIndexOf('.')) + ".asm";
                    includeStartupCode = false;
                }
                else
                    throw new Exception("File must have .vm extension.");
            }
            else {
                // Get all .vm files in directory
                files = pathname.listFiles((File p) -> p.isFile() && p.getName().endsWith(".vm"));
                
                // Check files were found
                if (files.length == 0)
                    throw new Exception("No .vm files found in directory '" + args[0] + "'.");
                
                outputPathname = java.nio.file.Path.of(pathname.getPath(), pathname.getName()).toString() + ".asm";
            }

            // Start translating
            codeWriter = new CodeWriter(outputPathname, includeStartupCode);
            for (int i = 0; i < files.length; i++) {
                codeWriter.setFilename(files[i].getName());
                parser = new Parser(files[i]);
                translate(parser, codeWriter);
                parser.close();
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (parser != null)
                try {
                    parser.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            if (codeWriter != null)
                try {
                    codeWriter.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
        }
    }

    private static void translate(Parser parser, CodeWriter codeWriter) throws Exception {
        while (parser.hasMoreCommands()) {
            // Move to next command 
            parser.advance();

            // Write translation of command to output file
            switch (parser.getCommandType()) {
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.getArg1());
                    break;
                case C_PUSH:
                case C_POP:
                    codeWriter.writePushPop(parser.getCommandType(), parser.getArg1(), parser.getArg2());
                    break;
                case C_LABEL:
                    codeWriter.writeLabel(parser.getArg1());
                    break;
                case C_GOTO:
                    codeWriter.writeGoto(parser.getArg1());
                    break;
                case C_IF:
                    codeWriter.writeIf(parser.getArg1());
                    break;
                case C_FUNCTION:
                    codeWriter.writeFunction(parser.getArg1(), parser.getArg2());
                    break;
                case C_CALL:
                    codeWriter.writeCall(parser.getArg1(), parser.getArg2());
                    break;
                case C_RETURN:
                    codeWriter.writeReturn();
                    break;
                default:
                    throw new Exception("Invalid command type.");
            }
        }
    }
}