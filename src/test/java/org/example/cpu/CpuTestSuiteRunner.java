package org.example.cpu;

import org.example.GameBoy;
import org.example.Main;
import org.example.cpu.tests.CpuTestCase;
import org.example.mmu.MMU;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class CpuTestSuiteRunner {

    private static final String TEST_PACKAGE = "org.example.cpu.tests.cases";

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("          ENGINE DI TEST POLIMORFICO SM83         ");
        System.out.println("==================================================");

        List<CpuTestCase> tests = discoverTestCases(TEST_PACKAGE);

        Main.DEBUG = false;

        GameBoy gameBoy = new GameBoy();

        MMU mmu = gameBoy.getMmu();
        SM83 cpu = gameBoy.getCpu();

        int totalClasses = tests.size();
        int passedClasses = 0;
        int totalAssertionsMade = 0;

        List<TestFailure> globalFailures = new ArrayList<>();

        for (CpuTestCase testCase : tests) {
            TestReporter reporter = new TestReporter();

            try {
                testCase.execute(cpu, mmu, reporter);

                totalAssertionsMade += reporter.getTotalAssertions();

                if (!reporter.hasFailed()) {
                    passedClasses++;
                    System.out.printf("[PASS] %s (%d verifiche OK)%n", testCase.getName(), reporter.getTotalAssertions());
                } else {
                    globalFailures.addAll(reporter.getFailures());
                    System.out.printf("[FAIL] %s (%d fallimenti riscontrati)%n", testCase.getName(), reporter.getFailures().size());
                }
            } catch (Exception e) {
                // Protezione da crash strutturali (NullPointer, IndexOutOfBounds...) nel codice del test
                System.out.printf("[CRASH] %s: Eccezione imprevista durante il test! (%s)%n", testCase.getName(), e.getMessage());
            }
        }

        // --- REPORT FINALE ---
        System.out.println("\n==================================================");
        System.out.println("                SUMMARY DEI TEST                  ");
        System.out.println("==================================================");
        System.out.printf("Classi Test: %d/%d Superate | Verifiche Totali: %d%n", passedClasses, totalClasses, totalAssertionsMade);

        if (!globalFailures.isEmpty()) {
            System.out.println("--------------------------------------------------");
            System.out.println("LOG DETTAGLIATO DEGLI ERRORI:");
            for (TestFailure failure : globalFailures) {
                System.err.printf(" -> Opcode 0x%02X: %s%n", failure.opcode(), failure.description());
            }
        } else {
            System.out.println("Tutte le classi di test hanno validato il silicio con successo!");
        }
        System.out.println("==================================================");
    }

    private static List<CpuTestCase> discoverTestCases(String packageName) {
        List<CpuTestCase> discoveredTests = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            URL resource = classLoader.getResource(path);
            if (resource == null) return discoveredTests;

            // Soluzione pulita e robusta tramite URI: gestisce spazi (%20) e caratteri speciali nativamente
            File directory = new File(resource.toURI());

            if (!directory.exists()) return discoveredTests;

            File[] files = directory.listFiles();
            if (files == null) return discoveredTests;

            for (File file : files) {
                // Cerchiamo solo i file compilati .class
                if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);

                    // Verifichiamo se la classe implementa CpuTestCase e non è una classe astratta o interfaccia
                    if (CpuTestCase.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        // Creiamo un'istanza al volo usando il costruttore di default senza parametri
                        CpuTestCase testInstance = (CpuTestCase) clazz.getDeclaredConstructor().newInstance();
                        discoveredTests.add(testInstance);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore durante la scansione della Reflection: " + e.getMessage());
        }

        return discoveredTests;
    }
}