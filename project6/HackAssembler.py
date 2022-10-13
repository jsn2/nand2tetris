import re, sys

RE_SYM_LABEL = "^\([A-Za-z_.:$]+[0-9A-Za-z_.:$]*\)$"
RE_A_INSTR_SYM = "^@[A-Za-z_.:$]+[0-9A-Za-z_.:$]*$"
RE_A_INSTR = "^@[0-9]+$"
RE_C_INSTR = "^((M|D|MD|A|AM|AD|AMD)=)?(0|1|-1|D|A|M|!D|!A|!M|-D|-A|-M|D\+1|A\+1|M\+1|D-1|A-1|M-1|D\+A|D\+M|D-A|D-M|A-D|M-D|D&A|D&M|D\|A|D\|M)(;|(;J(GT|EQ|GE|LT|NE|LE|MP)))?$"

class Assembler:
    # Constructor
    def __init__(self, src_filename):
        self.symbols = {
            "R0": 0, "R1": 1, "R2": 2, "R3": 3,
            "R4": 4, "R5": 5, "R6": 6, "R7": 7,
            "R8": 8, "R9": 9, "R10": 10, "R11": 11,
            "R12": 12, "R13": 13, "R14": 14, "R15": 15,
            "SCREEN": 16384, "KBD": 24576,
            "SP": 0, "LCL": 1, "ARG": 2, "THIS": 3, "THAT": 4
        }
        self.dest = {
            "null": "000", "M": "001", "D": "010", "MD": "011",
            "A": "100", "AM": "101", "AD": "110", "AMD": "111"
        }
        self.jmp = {
            "null": "000", "JGT": "001", "JEQ": "010", "JGE": "011",
            "JLT": "100", "JNE": "101", "JLE": "110", "JMP": "111"
        }
        self.comp = {
            "0": "101010", "1": "111111", "-1": "111010", "D": "001100",
            "A": "110000", "M": "110000", "!D": "001101", "!A": "110001",
            "!M": "110001", "-D": "001111", "-A": "110011", "-M": "110011",
            "D+1": "011111", "A+1": "110111", "M+1": "110111", "D-1": "001110",
            "A-1": "110010", "M-1": "110010", "D+A": "000010", "D+M": "000010",
            "D-A": "010011", "D-M": "010011", "A-D": "000111", "M-D": "000111",
            "D&A": "000000", "D&M": "000000", "D|A": "010101", "D|M": "010101"
        }
        self.src_filename = src_filename
        self.address = 16


    # Scan file for label symbols (xxx) and add to symbol dict
    def scan_labels(self):
        instr_line_num = -1
        result = 0

        try:
            src_f = None
            src_f = open(self.src_filename, "rt")
            line = src_f.readline()
            while line != "":
                # Strip comment from line
                com_pos = line.find("//")
                if com_pos != -1:
                    line = line[:com_pos]

                # Strip all whitespace
                line = re.sub("\s", "", line)

                # If line is not empty
                if line != "":
                    # Try extract label
                    label = re.search(RE_SYM_LABEL, line)

                    if label == None:
                        # Not a label so increment line num
                        instr_line_num += 1
                    else:
                        # Label so add to dict
                        self.symbols.update({re.sub("[\(\)]", "", label.group()): instr_line_num + 1})
                line = src_f.readline()
        except OSError as e:
            print("ERROR: failed to process the source or target file.\nAborted.")
            result = 1
        except:
            print("ERROR: no specific details.\nAborted.")
            result = 2
        finally:
            if src_f != None:
                src_f.close()

        return result
    
    # Convert instr to 16-bit binary equivalent
    def translate(self, instr):
        binstr = None

        # Check if A-instruction without symbol
        match = re.search(RE_A_INSTR, instr)
        if match != None:
            # Convert val into 16-bit value (0 prefix for A-instruction)
            val = match.group().replace("@", "")
            binstr = "0" + format(int(val), "015b")
        else:
            # Check if A-instruction with symbol
            match = re.search(RE_A_INSTR_SYM, instr)
            if match != None:
                # Add symbol to symbols dict if doesn't exist
                key = match.group().replace("@", "")
                if key in self.symbols:
                    val = self.symbols[key]
                else:
                    self.symbols.update({key: self.address})
                    val = self.address
                    self.address += 1

                # Convert val into 16-bit value (0 prefix for A-instruction)
                binstr = "0" + format(int(val), "015b")
            else:
                # Check if C-instruction
                match = re.search(RE_C_INSTR, instr)
                if match != None:
                    cmd = match.group()

                    # Get dest
                    dest = cmd[:cmd.find("=")] if "=" in cmd else "null"

                    # Get jmp
                    jmp = cmd[cmd.find(";") + 1:] if ";" in cmd else ""
                    jmp = "null" if jmp == "" else jmp

                    # Get comp
                    comp = cmd.replace(dest + "=", "").replace(";" + jmp, "").replace(";", "")

                    # Build binstr (111 ac1c2c3c4c5c6 d1d2d3 j1j2j3)
                    binstr = "111"
                    binstr += "1" if "M" in comp else "0"
                    binstr += self.comp[comp]
                    binstr += self.dest[dest]
                    binstr += self.jmp[jmp]

        return binstr
    
    # Assemble
    def assemble(self):
        self.address = 16
        result = 0

        # Set target filename
        tar_filename = self.src_filename
        tar_filename = tar_filename[:tar_filename.rfind(".")] if "." in tar_filename else tar_filename
        tar_filename += ".hack"

        try:
            # Open files
            src_f = None
            tar_f = None
            src_f = open(self.src_filename, "rt")
            tar_f = open(tar_filename, "wt")

            # For each instruction in source file, translate and write to target
            line = src_f.readline()
            while line != "":
                # Strip comment from line
                com_pos = line.find("//")
                if com_pos != -1:
                    line = line[:com_pos]

                # Strip all whitespace
                line = re.sub("\s", "", line)

                # Translate instr to 16-bit binary equivalent
                binstr = self.translate(line)

                # If A or C instruction 
                if binstr != None:
                    # Write to target file
                    tar_f.write(binstr + "\n")
                line = src_f.readline()
        except OSError:
            print("ERROR: An error occurred while attempting to process the source or target file.\nAborted.")
            result = 3
        except:
            print("ERROR: no specific details.\nAborted.")
            result = 4
        finally:
            # Close files
            if tar_f != None:
                tar_f.close()
            if src_f != None:
                src_f.close()

        return result

def main():
    if len(sys.argv) != 2:
        print("Usage: " + sys.argv[0] + " " + "filename")
    else:
        assembler = Assembler(sys.argv[1])
        
        print("Scanning...")
        result = assembler.scan_labels()

        if result == 0:
            print("Translating...")
            result = assembler.assemble()

            if result == 0:
                print("Done.")

main()