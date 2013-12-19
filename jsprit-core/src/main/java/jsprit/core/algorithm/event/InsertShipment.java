package jsprit.core.algorithm.event;

import jsprit.core.problem.job.Shipment;
import jsprit.core.problem.solution.route.VehicleRoute;

public class InsertShipment implements RouteChangedEvent{

	private Shipment shipment;
	
	private int pickupIndex;
	
	private int deliveryIndex;
	
	public InsertShipment(VehicleRoute route2change, Shipment shipment, int pickupIndex, int deliveryIndex) {
		this.shipment = shipment;
		this.pickupIndex = pickupIndex;
		this.deliveryIndex = deliveryIndex;
	}

	/**
	 * @return the shipment
	 */
	public Shipment getShipment() {
		return shipment;
	}

	/**
	 * @return the pickupIndex
	 */
	public int getPickupIndex() {
		return pickupIndex;
	}

	/**
	 * @return the deliveryIndex
	 */
	public int getDeliveryIndex() {
		return deliveryIndex;
	}
	
	

}