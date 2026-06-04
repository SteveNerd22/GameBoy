package org.example.cpu;

public class OpcodeTestException extends RuntimeException {
    private final int opcode;
    private final String testName;
    private final String detail;

    public OpcodeTestException(int opcode, String testName, String detail) {
        super(String.format("Fallimento Opcode 0x%02X [%s]: %s", opcode, testName, detail));
        this.opcode = opcode;
        this.testName = testName;
        this.detail = detail;
    }

    public int getOpcode() { return opcode; }
    public String getTestName() { return testName; }
    public String getDetail() { return detail; }
}