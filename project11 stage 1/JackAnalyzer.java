import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class JackAnalyzer {
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java JackAnalyzer filename/directory");
            System.exit(1);
        }

        // Init to null to avoid compiler warnings.
        JackTokenizer tokenizer = null;
        BufferedWriter writer = null;
        try {
            File pathname = new File(args[0]).getCanonicalFile();

            // Check if file or directory exists.
            if (!pathname.exists() || !(pathname.isFile() || pathname.isDirectory()))
                throw new Exception("File or directory '" + args[0] + "' not found.");
            
            // Get input and output files.
            File[] inputFiles;
            if (pathname.isFile()) {
                // Check if file is valid.
                if (pathname.getName().endsWith(".jack")) {
                    inputFiles = new File[1];
                    inputFiles[0] = new File(pathname.getPath());
                }
                else
                    throw new Exception("File must have .jack extension.");
            }
            else {
                // Get all .jack files in directory.
                inputFiles = pathname.listFiles((File p) -> p.isFile() && p.getName().endsWith(".jack"));
                
                // Check if files were found.
                if (inputFiles.length == 0)
                    throw new Exception("No .jack files found in directory '" + args[0] + "'.");
            }

            // Start translating.
            CompilationEngine compEngine;
            for (int i = 0; i < inputFiles.length; i++) {
                tokenizer = new JackTokenizer(inputFiles[i]);
                writer = Files.newBufferedWriter((new File(inputFiles[i].getPath().substring(0, inputFiles[i].getPath().lastIndexOf('.')) + ".xml")).toPath(), StandardCharsets.UTF_8);
                compEngine = new CompilationEngine(tokenizer, writer);
                compEngine.compileClass();

                tokenizer.close();
                writer.close();
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (tokenizer != null) {
                try {
                    tokenizer.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}