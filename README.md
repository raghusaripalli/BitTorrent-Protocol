### CNT5106 BitTorrent - P2P File sharing 

**Members:**

Raghuveer Sharma Saripalli (UFID: 50946752)
Amith Kamath (UFID: 66961168)
Madhuri Uppu (UFID: 43029778)


NOTE:
All the messages in the log are according to what was described in the project description pdf. We are not printing 
bitfields because the log file will grow too large for small piece sizes where total number of pieces will drastically increase


We were not able to succeed with remotely starting the peer process. Hence we manually started the process in each server as shown
in the demo. Hence we have NOT included the StartRemotePeer since we never executed it remotely

**Running the project:**
1. Make sure all the source files are in the same folder
2. Compile the program - javac peerProcess.java
2. Compile the program - javac CheckEquality.java
3. Make sure Common.cfg and PeerInfo.cfg has appropriate parameters set and is in the same folder as source files
4. Start the peer process in each server with the command: 	java peerProcess {peerId}
5. After execution, to check if files are equal, execute: 	java CheckEquality

If at all Peers are being run on different machines, make sure the piece size is small enough and from our experience,
it is better to have the piece size less than or equal to 1000 bytes


Video demo link - 
dropbox:  https://www.dropbox.com/sh/5x20l2wh9u5t1i7/AACFMBOEfyp40Cj60kPrZknOa?dl=0
Onedrive: https://uflorida-my.sharepoint.com/:v:/g/personal/madhuri_uppu_ufl_edu/EYw3hGz7_qhFu8MnCYorYZgBAusxHdmyDnNOj7UwkG2P2A?e=thDqVf


The design of the application, specifically with respect to communication with the peers via threads, structruing of the messages
and abstractions while creating a message was disucssed by everyone


	a) Handling of choke and unchoke messages
	b) Selection of preferred neighbor based on downloaded rates and selection of optimistically unchoked neighbor
	c) Handling scenario where an unchoked peer requests for a piece, only to find that it has been choked. In such case, the
	   peer does not receive the piece. This is handled by running a thread that periodically checks for disparity between the
	   requested bitfield state and pieces received bitfield state
	d) Monitoring the state of other peers and terminating message listener threads and the program when all the peers have 
	   downloaded the file
	e) Handling Request, Piece, Have, Interested, Not Interested messages
	f) File handling with respect to pieces i.e to read piece bytes from the file and write piece bytes to the file
	g) Logic/implementation for converting bytes to message and vice versa
	h) Application level logger implementation
	i) Parsing the configurations and setting application level properties
	j) Maintaining sockets and threads - creating listener thread for accepting new connections and Message listener threads
	k) Initial handshake and bitfield exchange
	l) Reading data from the socket
