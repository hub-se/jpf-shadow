# Shadow Symbolic Execution with Java PathFinder

ShadowJPF (jpf-shadow) is an extension of the Java PathFinder (JPF) [1,2] and applies the idea of shadow symbolic execution [3] to Java bytecode and, hence, allows to detect divergences in Java programs that expose new program behavior.

This tool was presented at the [Java PathFinder Workshop 2017](https://jpf.byu.edu/jpf-workshop-2017/) workshop co-located with the ASE. For more information please have a look at our workshop paper published in the [ACM SIGSOFT Software Engineering Notes](https://dl.acm.org/citation.cfm?id=3149492).

Authors:
Yannic Noller, Hoang Lam Nguyen, Minxing Tang, and Timo Kehrer

<sub> [1] W. Visser, K. Havelund, G. Brat, S. Park, and F. Lerda. Model checking programs. Automated Software Engineering, 10(2):203–232, Apr 2003. https://doi.org/10.1023/A:1022920129859 </sub>
<br>
<sub> [2] C. S. Pasareanu, W. Visser, D. Bushnell, J. Geldenhuys, P. Mehlitz, and N. Rungta. Symbolic pathfinder: integrating symbolic execution with model checking for java bytecode analysis. Automated Software Engineering, 20(3):391–425, 2013. https://doi.org/10.1007/s10515-013-0122-2 </sub>
<br>
<sub> [3] Hristina Palikareva, Tomasz Kuchta, and Cristian Cadar. 2016. Shadow of a doubt: testing for divergences between software versions. In Proceedings of the 38th International Conference on Software Engineering (ICSE '16). ACM, New York, NY, USA, 1181-1192. https://doi.org/10.1145/2884781.2884845 </sub>

## Setup

The following instructions will show you how to install and run `jpf-shadow`.

### 1. Prerequisites

jpf-shadow is built as an extension of Symbolic Pathfinder (SPF) (which is again build on top of Java Pathfinder(JPF)). Therefore, make sure to have [jpf-core](https://github.com/javapathfinder/jpf-core) and [jpf-symbc](https://github.com/SymbolicPathFinder/jpf-symbc) installed and properly setup on your machine.  

Assuming that you have installed `jpf-core`, `jpf-symbc` and `jpf-shadow` under `<user.home>/workspace/jpf`, your `site.properties` file (usually located in `<user.home>/.jpf`) should look like this:

```
# JPF site configuration

jpf-core = ${user.home}/workspace/jpf/jpf-core
jpf-symbc = ${user.home}/workspace/jpf/jpf-symbc
jpf-shadow = ${user.home}/workspace/jpf/jpf-shadow

extensions=${jpf-core},${jpf-symbc},${jpf-shadow}

```

### 2a. Setup using the command line

To build jpf-shadow using the command line, move to the project directory (i.e. `<user.home>/workspace/jpf/jpf-shadow` and run: 

```
ant build
```
If there are any building errors, please check if your `site.properties` file is setup correctly.

### 2b. Setup using Eclipse

1. In Eclipse, go to **File > Import...** to import `jpf-shadow` as an existing project.
2. Confirm that `jpf-core` and `jpf-symbc` are listed as required projects on the build path of `jpf-shadow`.
3. Select the `build.xml` file and run it as **Ant Build**.


### 3. Running jpf-shadow 
To run a `change()`-annotated Java program, jpf-shadow requires an appropriate `.jpf` configuration file. The folder `src/examples` contains various examples of annotated programs with their `.jpf` files.

The settings that can be specified are:

| Parameter            | Description | Mandatory
| -------------------- |-------------| ---------
| jpf.target           | Qualified name of the class to analyze. | Yes
| jpf.classpath        | Path to the application binaries. | Yes
| symbolic.method      | Qualified name of the method(s) to run using shadow symbolic execution. | Yes
| listener = gov.nasa.jpf.shadow.ShadowListener | Use the ShadowListener to monitor the execution. | Yes
| symbolic.optimizechoices = false | Turn off optimizations for certain instructions. | Yes
| symbolic.dp          | Decision Procedure for constraint solving. Recommended: "z3" Alternatives: "coral", "choco".	| Yes
| symbolic.max_int     | Maximum value of symbolic integers. | No
| symbolic.min_int     | Minimum value of symbolic integers. | No
| search.multiple_errors = true | If an error is found, continue with the search. | No
| search.depth_limit 	| Limit the search depth, e.g. 10. | No

#### Example: Foo
The `Foo` example from the paper (located inside `src/examples/jpf2017`) can now be executed in the following way:

##### 1. Choosing the initial test input
To specify the initial test input of the `foo()` method, simply change the concrete parameter passed to it inside the main method.

##### 2a. Run using the command line
Use the command:
```
java -jar <user.home>/workspace/jpf/jpf-core/build/RunJPF.jar <user.home>/workspace/jpf/jpf-shadow/src/examples/jpf2017/foo/Foo.jpf
```
##### 2b. Run in Eclipse
Select the `Foo.jpf` configuration file and run it using the **run-jpf-shadow** run configuration. 

##### 3. Output
Assuming that `foo` has been run with the initial input `x=1`, the output should contain the following lines:

```
...
====================================================== Method Summaries
Inputs: x

jpf2017.foo.Foo_1c.foo(11)	--> Diff in line: 22, Type: diffFalse	(Return Value: 2)
Paths explored: 1
...
```

This means that `jpf-shadow` has found a _diff false_ divergence (i.e. the old version takes the _true_ path while the new version takes the _false_ path) at line 22 for the input `x=11` and the return value (of the new version) is 2.



