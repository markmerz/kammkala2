# kammkala2

I'm publishing parts of project "Kammkala" which were written for my master thesis in 2012 and were funded by IT-department of University of Tartu and STACC.

"Kammkala" solves problem of complex Ethernet topology detection using information from network switches gathered by SNMP probes.

Most of the code base is for data gathering and presentation. In "Kammkala-web" folder is simple java web application for management tasks. In "kammkala2-daemon" is a daemon for data gathering. 

Most interesting part of the project is in "kammkala2-daemon/src/ee/ut/merzin/kammkala2/logic/Topology.java" and "kammkala2-daemon/src/ee/ut/merzin/kammkala2/logic/LongestPathReduction.java" files. Those files contain implementation of the topology solving algorithm.

I felt fascinated with the problem since late 90s then I started working as a net-admin. By the time I acquired education and skills for actually solving it, the market had invented and incorporated CDP and LLDP protocols so my solution was already obsolete. Better luck next time!

