package org.toyotatransportationplanning;

import java.util.HashMap;
import java.util.Map;

public class TreeNode {
    Map<String, TreeNode> children = new HashMap<>();
    Double value = null;

    public TreeNode() {
    }
}
