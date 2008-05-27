package hgtest.jxta;

import java.net.URI;
import java.net.URISyntaxException;

import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.PeerConfiguration;
import org.hypergraphdb.peer.jxta.JXTAPeerConfiguration;

public class HyperGraphDBServer {
	public static void main(String[] args){
		System.out.println("Starting a HGDB server ...");

		JXTAPeerConfiguration jxtaConf = new JXTAPeerConfiguration("urn:jxta:uuid-59616261646162614E50472050325033C0C1DE89719B456691A596B983BA0E1004");
		
		PeerConfiguration conf = new PeerConfiguration(true, "./TestDB", 
				true, "org.hypergraphdb.peer.jxta.JXTAServerInterface", jxtaConf, 
				false, null, null);
		
		HyperGraphPeer server = new HyperGraphPeer(conf);
		
		server.start();

		while(true){
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
		}
	}
}
