package ro.unitbv.sma.agents;

import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ro.unitbv.sma.interfaces.BuyGui;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BuyerAgent extends Agent {
    private String productToBuy;
    private AID[] sellerAgentsList;
    private static final int REQUEST_TIME = 10000;
    private BuyGui buyerInterface;
	TopicManagementHelper topicHelper ;
	final AID topic = topicHelper.createTopic("history");

    //initialization method
    protected void setup() {
        System.out.println("Buyer Agent " + getAID().getName() + " was created.");
        try {
			topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        buyerInterface = new BuyGui(this);
        buyerInterface.showGui();
        
    }

    //destroyer method
    protected void takeDown() {
        System.out.println("Buyer Agent " + getAID().getName() + " has finished.");
        buyerInterface.dispose();
        System.exit(0);
    }
    
    public void checkIfProductAvailable(final String productToBuy) {
    	this.productToBuy = productToBuy;
        System.out.println("Product wanted: " + productToBuy);

        // Add a TickerBehaviour that schedules a request to seller agents every minute
        addBehaviour(new TickerBehaviour(this, REQUEST_TIME) {
            protected void onTick() {
                System.out.println("Trying to buy " + productToBuy);
				saveToHistory(productToBuy);
                // Update the list of seller agents
                DFAgentDescription agentDesc = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("product-sale");
                agentDesc.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, agentDesc);
                    System.out.println("Product found in the following seller agents: ");
                    sellerAgentsList = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        sellerAgentsList[i] = result[i].getName();
                        System.out.println(sellerAgentsList[i].getName());
                    }
                    System.out.println("--------");
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Perform the request
                myAgent.addBehaviour(new PurchaseRequest());
            }

			private void saveToHistory(final String productToBuy) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(topic);
				msg.setContent(String.valueOf(getTickCount()) + " " + this.getAgent().getLocalName() + " : " + "Trying to buy " + productToBuy);
				myAgent.send(msg);
			}
        });
    }
    
        /**
     * Inner class RequestPerformer.
     * This is the behaviour used by buyer agents
     * to request seller agents the target book.
     */
    private class PurchaseRequest extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgentsList.length; ++i) {
                        cfp.addReceiver(sellerAgentsList[i]);
                    }
                    cfp.setContent(productToBuy);
                    cfp.setConversationId("Product-Commerce");
                    cfp.setReplyWith("cfp " + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Product-Commerce"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgentsList.length) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(productToBuy);
                    order.setConversationId("Product-Commerce");
                    order.setReplyWith("Order " + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Product-Commerce"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(productToBuy + " has been purchased to agent " + reply.getSender().getName());
                            System.out.println("Price = " + bestPrice);
                            myAgent.doDelete();
                        } else {
                            System.out.println("Error: product requested was already sold");
                        }
                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Error: " + productToBuy + " is not for sale.");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
    
}
