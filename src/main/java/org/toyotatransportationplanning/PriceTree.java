package org.toyotatransportationplanning;

public class PriceTree {
    private final TreeNode root;

    public static final double PRICE_INF = Double.POSITIVE_INFINITY;

    public PriceTree() {
        root = new TreeNode();
    }

    public void insert(String transportUnit, String startPoint, String endPoint, String vehicle, double price) {
        TreeNode node = root;
        String[] keys = {transportUnit, startPoint, endPoint, vehicle};
        for (String key : keys) {
            node = node.children.computeIfAbsent(key, k -> new TreeNode());
        }
        node.value = price;
    }

    public double query(String transportUnit, String startPoint, String endPoint, String vehicle) {
        TreeNode node = root;
        String[] keys = {transportUnit, startPoint, endPoint, vehicle};
        for (String key : keys) {
            node = node.children.get(key);
            if (node == null) {
                return PRICE_INF;
            }
        }
        return node.value != null ? node.value : PRICE_INF;
    }
}

