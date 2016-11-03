package com.googlecode.fascinator;

import java.io.File;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;

import org.codehaus.groovy.control.CompilationFailedException;

import com.googlecode.fascinator.common.FascinatorHome;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * <p>
 * UpgradeClient class to run a script that aids administrators to upgrade the
 * application configuration for the new release.
 *
 * </p>
 *
 * <p>
 * By default runs upgrade.groovy in the ${fascinator.home}/upgrade directory
 * but can run a script of a different name by passing it as a parameter on the
 * commandline.
 * </p>
 *
 * @author Andrew Brazzatti
 */
public class UpgradeClient {
    private static final String DEFAULT_SCRIPT_NAME = "upgrade.groovy";
    private String groovyScript = null;
    private GroovyShell groovyShell = null;

    public UpgradeClient(String groovyScript) {
        this.groovyScript = groovyScript;
        Binding binding = new Binding();
        binding.setVariable("util", this);
        groovyShell = new GroovyShell(binding);
    }

    public UpgradeClient() {
        this(DEFAULT_SCRIPT_NAME);
    }

    public static void main(String[] args)
            throws CompilationFailedException, IOException {

        UpgradeClient client;
        if (args.length > 0 && args[0] != null) {
            client = new UpgradeClient(args[0]);
        } else {
            client = new UpgradeClient();
        }
        client.run();

    }

    private void run() throws CompilationFailedException, IOException {
        groovyShell.evaluate(FascinatorHome
                .getPathFile("upgrade" + File.separator + groovyScript));
    }

    /**
     * Util method to prompt a user for input
     *
     * @param question The question to ask the user
     * @param suggestion Suggested response to supply to the user (e.g. [Y/N])
     * @param responsePattern A regular expression to ensure the response is
     *            valid. Will prompt the question again if the pattern doesn't
     *            match
     * @return
     */
    public String promptUserInput(String question, String suggestion,
            String responsePattern) {
        try {
            System.out.println(question);
            System.out.print(suggestion + " ");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.next(responsePattern);
            scanner.close();
            return line;
        } catch (InputMismatchException e) {
            promptUserInput(question, suggestion, responsePattern);
        }

        return "";
    }

}
