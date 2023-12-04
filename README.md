# Group 42 Playbook

a. **Group Number**: 42  
b. **Names**:
- Jim Su (jimsu@ufl.edu)
- Shehzad Shah (shehzadshah@ufl.edu)
- Alejandro Leon (leon.alejandro@ufl.edu)
- Andy Zhang (@ufl.edu)

c. **Roles**:
- Jim: Reading configuration files, Bitfield class interface design, populating peer hashmaps, concurrency, logging
- Shehzad: All-around development, receive & send functions for each data type, testing, debugging, and documentation
- Alejandro: Worked on technical details for storing and tracking peer data locally
- Andy:

d. **Video Link**: https://youtu.be/o6h0ZdDticY

e. **Project Achievements**:
After lots of work, we were able to achieve all of the project requirements and create a flawless simulation. That said, we are running this locally (following the messages on Slack) because Storm limits to 4 ssh sessions, and we needed 6.

f. **Setup Instructions**:
Unzip the file (it is a standard tgz file). Then, enter the project root, enter the `src/` directory and run `javac peerProcess.java`, then `java peerProcess <peerID>` for each peer.

Before the above will work, you need to have `peer_1001`, `peer_1002`, etc. directories in the root of the project. Peers that start with the file must have it in their `peer_X` directory.

You also need a `src/PeerInfo.cfg` file, which contains the peer ID, host name, and listening port for each peer. The `src/Common.cfg` file should also be in the root of the project. This file contains the number of preferred neighbors, unchoking interval, optimistic unchoking interval, file size, and piece size.

We have included an example of each of these files in the root of the project.

g. **GitHub Link**: https://github.com/jimsu2012/cnt4007