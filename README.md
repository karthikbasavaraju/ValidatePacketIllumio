# ValidatePacketIllumio
To validate packets based on set of rules

To run the program call new Illumio("path") constructor with path of csv file(The file should only incude valid rules and file should not contain headers).
Call acceptPacket("direction","protocol","port","Ipaddress") to know whether the packet is valid or not based on rules.

My algorithm is based on non-overlapping range represented as a tree structure to reduce the time and space complexity of program.
Every node of tree represent a non-overlapping range of a certain parameter(port or ip address). The validation goes to next level only if the cur level range satisfies the rule. The ranges are split into smaller ranges when a new range overlaps an existing range and with different parameters.
If I had more time, then I would have included “merge” function to merge parameters which can form a continue range, which would reduce the space complexity even more. The space complexity in worst case is n*m(Where n is range of parameter and m is depth of range). Time complexity to build the tree is n*m. Time complexity to check whether the packet is valid or not is log(k), where k is number of ranges in a node.
The core part of algorithm is building the tree. Once the tree is built checking whether a packet is valid or not is straight forward(dfs traversal). Since building is the core part. I tested my algorithm by giving every possible overlapping and non overlapping ranges.
The program doesnt handle invalid inputs.
