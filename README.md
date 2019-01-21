# ValidatePacketIllumio
To validate packets based on set of rules

To run the program call new Illumio("path") constructor with path of csv file(The file should only incude valid rules and file should not contain headers).
Call acceptPacket("direction","protocol","port","Ipaddress") to know whether the packet is valid or not based on rules.

The rules are stored as tree structure where nodes contains non-overlapping ranges of a parameter.
