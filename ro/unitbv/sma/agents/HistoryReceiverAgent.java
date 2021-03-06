/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package ro.unitbv.sma.agents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class HistoryReceiverAgent extends Agent {

	@Override
	protected void setup() {
		try {			
			addBehaviour(new CyclicBehaviour(this) {
				public void action() {
					ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					if (msg != null) {
						String message = "Agent "+ myAgent.getLocalName()+" message " + msg.getContent() + " at time: " + msg.getPostTimeStamp() + " received." + "\n";
						try {
						    Files.write(Paths.get("historyOfActions.txt"), message.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
						}catch (IOException e) {
							e.printStackTrace();
						}
					}
					else {
						block();
					}
				}
			} );
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}