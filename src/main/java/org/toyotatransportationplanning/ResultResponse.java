package org.toyotatransportationplanning;

import java.util.List;

public class ResultResponse {
    private final List<Order> orders;
    private final double totalPrice;
    private final int carCount;

    public ResultResponse(List<Order> orders, double totalPrice, int carCount) {
        this.orders = orders;
        this.totalPrice = totalPrice;
        this.carCount = carCount;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getCarCount() {
        return carCount;
    }
}
