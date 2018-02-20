# Shadow Symbolic Execution with Java PathFinder

ShadowJPF is an extension of the Java PathFinder (JPF) [1,2] and applies the idea of shadow symbolic execution [3] to Java bytecode and, hence, allows to detect divergences in Java programs that expose new program behavior.

This tool was presented at the [Java PathFinder Workshop 2017](https://jpf.byu.edu/jpf-workshop-2017/) workshop co-located with the ASE. For more information please have a look at our workshop paper published in the [ACM SIGSOFT Software Engineering Notes](https://dl.acm.org/citation.cfm?id=3149492).

Authors:
Yannic Noller, Hoang Lam Nguyen, Minxing Tang, and Timo Kehrer

<sub> [1] W. Visser, K. Havelund, G. Brat, S. Park, and F. Lerda. Model checking programs. Automated Software Engineering, 10(2):203–232, Apr 2003. https://doi.org/10.1023/A:1022920129859 </sub>
<br>
<sub> [2] C. S. Pasareanu, W. Visser, D. Bushnell, J. Geldenhuys, P. Mehlitz, and N. Rungta. Symbolic pathfinder: integrating symbolic execution with model checking for java bytecode analysis. Automated Software Engineering, 20(3):391–425, 2013. https://doi.org/10.1007/s10515-013-0122-2 </sub>
<br>
<sub> [3] Hristina Palikareva, Tomasz Kuchta, and Cristian Cadar. 2016. Shadow of a doubt: testing for divergences between software versions. In Proceedings of the 38th International Conference on Software Engineering (ICSE '16). ACM, New York, NY, USA, 1181-1192. https://doi.org/10.1145/2884781.2884845 </sub>
