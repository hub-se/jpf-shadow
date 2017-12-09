package gov.nasa.jpf.shadow;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores execution configuration for experiments.
 * 
 * @author Yannic Noller <nolleryc@gmail.com> - YN
 *
 */
@SuppressWarnings("serial")
public class SymExParameter {

    public String classpath;
    public String sourcepath;
    public String packageName;
    public String className;
    public String methodName;
    public String methodNameWithSymbolicParameter;
    public int numberOfClassesInBenchmark;
    public String specialBenchmarks;
    public String resultsDirectory;
    public Map<String, String> constraintSolver;

    public SymExParameter(String classpath, String sourcepath, String packageName, String className, String methodName,
            String methodNameWithSymbolicParameter, int numberOfClassesInBenchmark, String specialBenchmarks,
            String resultsDirectory, Map<String, String> constraintSolver) {
        this.classpath = classpath;
        this.sourcepath = sourcepath;
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.methodNameWithSymbolicParameter = methodNameWithSymbolicParameter;
        this.numberOfClassesInBenchmark = numberOfClassesInBenchmark;
        this.specialBenchmarks = specialBenchmarks;
        this.resultsDirectory = resultsDirectory;
        this.constraintSolver = constraintSolver;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("classpath=");
        sb.append(this.classpath);
        sb.append("\n");
        sb.append("sourcepath=");
        sb.append(this.sourcepath);
        sb.append("\n");
        sb.append("package=");
        sb.append(this.packageName);
        sb.append("\n");
        sb.append("class=");
        sb.append(this.className);
        sb.append("\n");
        sb.append("method=");
        sb.append(this.methodNameWithSymbolicParameter);
        sb.append("\n");

        return sb.toString();
    }

    // -----------------------

    public static final SymExParameter Foo = new SymExParameter("${jpf-shadow}/build/examples",
            "${jpf-shadow}/src/examples", "jpf2017.foo", "Foo", "foo", "foo(sym)", 1, "",
            "evaluation-results/00_Foo/shadow-results/", null);

    public static final SymExParameter BankAccount_deposit = new SymExParameter("${jpf-shadow}/build/examples",
            "${jpf-shadow}/src/examples", "jpf2017.bankaccount.deposit", "BankAccount", "deposit", "deposit(sym)", 8,
            "", "evaluation-results/01_BankAccount/deposit/shadow-results/", null);

    public static final SymExParameter BankAccount_withdraw = new SymExParameter("${jpf-shadow}/build/examples",
            "${jpf-shadow}/src/examples", "jpf2017.bankaccount.withdraw", "BankAccount", "withdraw",
            "withdraw(sym#sym)", 12, "", "evaluation-results/01_BankAccount/withdraw/shadow-results/", null);
   
    public static final SymExParameter BankAccount_main = new SymExParameter("${jpf-shadow}/build/examples",
            "${jpf-shadow}/src/examples", "jpf2017.bankaccount.main", "BankAccount", "main", "test(sym#sym#sym)", 23,
            "1_13,2_22,15_23,5_18,3_23,17_22,3_10_22,5_18_23", "evaluation-results/01_BankAccount/main/shadow-results/",
            new HashMap<String, String>() {
                {
                    put("19", "coral");
                }
            });

}
