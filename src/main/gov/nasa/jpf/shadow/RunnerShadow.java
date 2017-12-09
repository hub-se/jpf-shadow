package gov.nasa.jpf.shadow;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.shadow.MyPathCondition.PathResultType;
import gov.nasa.jpf.symbc.SymbolicListener;

/**
 * Experiment execution class.
 * 
 * @author Yannic Noller <nolleryc@gmail.com> - YN
 *
 */
public class RunnerShadow {

    public static void main(String[] args) {
        System.out.println(">> Started Runner for jpf-shadow ...");

        SymExParameter[] subjects = { SymExParameter.Foo, SymExParameter.BankAccount_deposit,
                SymExParameter.BankAccount_withdraw, SymExParameter.BankAccount_main, };

        try {
            runExperiments(subjects);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<MyPathCondition> executeShadowSymbolicExecutionForMethod(SymExParameter param) {
        ShadowListener shadowListener = null;
        try {
            Config conf = initConfig();
            conf.setProperty("classpath", param.classpath);
            conf.setProperty("sourcepath", param.sourcepath);
            conf.setProperty("target", param.packageName + "." + param.className);

            StringBuilder sb = new StringBuilder();
            for (String method : param.methodNameWithSymbolicParameter.split(",")) {
                sb.append(param.packageName);
                sb.append(".");
                sb.append(param.className);
                sb.append(".");
                sb.append(method);
                sb.append(",");
            }
            conf.setProperty("symbolic.method", sb.toString());

            if (param.constraintSolver == null) {
                conf.setProperty("symbolic.dp", "choco");
            } else {
                String currentSubject = param.className.substring(param.className.indexOf("_") + 1,
                        param.className.length() - 1);
                String solver = param.constraintSolver.get(currentSubject);
                if (solver == null) {
                    conf.setProperty("symbolic.dp", "choco");
                } else {
                    conf.setProperty("symbolic.dp", solver);
                }
            }

            JPF jpf = new JPF(conf);
            shadowListener = new ShadowListener(conf, jpf);
            jpf.addListener(shadowListener);
            jpf.run();
            if (jpf.foundErrors()) {
                System.out.println("#FOUND ERRORS = " + jpf.getSearchErrors().size());
            }
            return shadowListener.collectedPCs;
        } catch (JPFConfigException cx) {
            cx.printStackTrace();
            if (shadowListener != null) {
                return shadowListener.collectedPCs;
            }
        } catch (JPFException jx) {
            jx.printStackTrace();
            if (shadowListener != null) {
                return shadowListener.collectedPCs;
            }
        }
        return null;
    }

    private static Config initConfig() {
        Config conf = JPF.createConfig(new String[0]);
        conf.setProperty("symbolic.min_int", "-100");
        conf.setProperty("symbolic.max_int", "100");
        conf.setProperty("symbolic.min_double", "-100.0");
        conf.setProperty("symbolic.max_double", "100.0");
        conf.setProperty("symbolic.undefined", "-1000");
        conf.setProperty("search.multiple_errors", "true");
        conf.setProperty("jvm.insn_factory.class", "gov.nasa.jpf.symbc.SymbolicInstructionFactory");
        conf.setProperty("vm.storage.class", "nil");
        // conf.setProperty("search.depth_limit", "30"); // default = null, i.e.
        conf.setProperty("symbolic.optimizechoices", "false");
        return conf;
    }

    private static void printPathConditions(Set<MyPathCondition> pathConditions, SymExParameter param) {
        System.out.println(param);
        System.out.println();
        if (pathConditions == null) {
            System.out.println("Terminated unexpectedly with <null>.");
        } else if (pathConditions.isEmpty()) {
            System.out.println("PathCondition: FALSE");
        } else if (pathConditions.size() == 1 && pathConditions.contains(SymbolicListener.TRUE)) {
            System.out.println("PathCondition: TRUE");
        } else {
            int counterViolations = 0;
            for (MyPathCondition myPC : pathConditions) {
                if (myPC.equals(SymbolicListener.TRUE)) {
                    System.out.println("PathCondition: TRUE \n");
                } else if (myPC.pc != null) {
                    try {
                        boolean isSat = myPC.pc.solve();
                        System.out.println(myPC.pathResultType + "; " + "SAT = " + isSat);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        System.out.println(myPC.pathResultType + "; " + "SAT = UNKNOWN NPE");
                    }
                    Map<String, Object> solution = myPC.pc.solveWithValuation();
                    for (String var : solution.keySet()) {
                        System.out.println(var + "=" + solution.get(var));
                    }

                    System.out.println(myPC.pc + "\n");

                    if (myPC.pathResultType == PathResultType.PROPERTY_VIOLATED) {
                        counterViolations++;
                    }
                } else {
                    throw new RuntimeException("PC is null..");
                }
            }
            System.out.println();
            System.out.println("#PROPERTY_VIOLATED = " + counterViolations);
            System.out.println("#Total = " + pathConditions.size());
        }
    }

    public static void runExperiments(SymExParameter[] subjects) {

        Set<MyPathCondition> resultingPCs = null;
        for (SymExParameter subject : subjects) {

            // Run shadow symbolic execution for change-annotated files.
            for (int i = 1; i <= subject.numberOfClassesInBenchmark; i++) {

                SymExParameter symExParameter = new SymExParameter(subject.classpath, subject.sourcepath,
                        subject.packageName, subject.className + "_" + i + "c", subject.methodName,
                        subject.methodNameWithSymbolicParameter, 1, "", subject.resultsDirectory,
                        subject.constraintSolver);
                String logPath = symExParameter.resultsDirectory + "log-" + symExParameter.packageName + "."
                        + symExParameter.className + ".txt";
                try {
                    ensureFileAndDirectoryExists(logPath);
                    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(logPath)), true));
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                resultingPCs = executeShadowSymbolicExecutionForMethod(symExParameter);
                printPathConditions(resultingPCs, symExParameter);
            }

            if (!subject.specialBenchmarks.equals("")) {
                for (String specialBenchmark : subject.specialBenchmarks.split(",")) {
                    SymExParameter symExParameter = new SymExParameter(subject.classpath, subject.sourcepath,
                            subject.packageName, subject.className + "_" + specialBenchmark + "c", subject.methodName,
                            subject.methodNameWithSymbolicParameter, 1, "", subject.resultsDirectory,
                            subject.constraintSolver);
                    String logPath = symExParameter.resultsDirectory + "log-" + symExParameter.packageName + "."
                            + symExParameter.className + ".txt";
                    try {
                        ensureFileAndDirectoryExists(logPath);
                        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(logPath)), true));
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    resultingPCs = executeShadowSymbolicExecutionForMethod(symExParameter);
                    printPathConditions(resultingPCs, symExParameter);
                }
            }

        }
    }

    private static void ensureFileAndDirectoryExists(String path) throws IOException {
        File file = new File(path);
        if (!file.isFile()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

}