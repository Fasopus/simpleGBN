Receiver takes the following arguments in order:
	<Hostname for the network emulator>
	<UDP port used to send acks from>(A)
	<UDP port used to receive data>(B)
	<Name of output file to write data to>

Sender takes the following arguments in order:
	<Hostname for the network emulator>
	<UDP port used to send>(B)
	<UDP port used to receive acks>(A)
	<Name of file to be transfered>

Both are called with the format:
Java <Name of Program> <Arguments>

NOTE:This code was originally written as part of an assignment that used a network emulator. However it still works for directly connecting the hosts. 
Only 2 unique port #'s are required, A and B.
This simple implementation uses the same port number on each end for each communication type (data and acks)
	

Also note that several log files are generated in the running directory