package org.toyotatransportationplanning;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;

public class SolverUtil {
    public static int[] scip(int[] quantityEachSize, List<Integer> filteredCombinations,
                             String source, String dest, Date date, List<List<Integer>> capacity,
                             Map<Integer, String> capacity2unit, Map<Integer, String> capacity2vehicle,
                             Map<String, Map<String, Map<Date, Integer>>> deliveryQuantity,
                             PriceTree priceTree) {
        Loader.loadNativeLibraries();
        MPSolver solver = new MPSolver("SCIP_Solver", MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING);

        int n = filteredCombinations.size();
        if (n == 0) return new int[0];

        // Xác định số lượng xe tối đa có thể dùng
        int maxVehicle = 0;
        for (String unit : Arrays.asList("GRL", "PA", "PSC", "VTC")) {
            maxVehicle = Math.max(maxVehicle, deliveryQuantity.getOrDefault(source, new HashMap<>())
                    .getOrDefault(unit, new HashMap<>())
                    .getOrDefault(date, 0));
        }

        // Khởi tạo biến quyết định
        MPVariable[] result = new MPVariable[n];
        for (int j = 0; j < n; j++) {
            result[j] = solver.makeIntVar(0, maxVehicle, "result_" + j);
        }

        // Ràng buộc 1: Tổng số xe mỗi loại không vượt quá quantityEachSize
        for (int sizeIndex = 0; sizeIndex < 7; sizeIndex++) {
            MPConstraint constraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, quantityEachSize[sizeIndex]);
            for (int j = 0; j < n; j++) {
                constraint.setCoefficient(result[j], capacity.get(filteredCombinations.get(j)).get(sizeIndex));
            }
        }

        // Ràng buộc 2: Số xe thuộc mỗi đơn vị vận chuyển không vượt quá giới hạn deliveryQuantity
        for (String unit : Arrays.asList("GRL", "PA", "PSC", "VTC")) {
            MPConstraint constraint = solver.makeConstraint(0,
                    deliveryQuantity.getOrDefault(source, new HashMap<>())
                            .getOrDefault(unit, new HashMap<>())
                            .getOrDefault(date, 0)
            );
            for (int j = 0; j < n; j++) {
                if (capacity2unit.get(filteredCombinations.get(j)).equals(unit)) {
                    constraint.setCoefficient(result[j], 1);
                }
            }
        }

        // Hàm mục tiêu
        MPObjective objective = solver.objective();
        double weight1 = 1.0, weight2 = 0.55;
        for (int j = 0; j < n; j++) {
            int idx = filteredCombinations.get(j);
            String unit = capacity2unit.get(idx);
            String vehicle = capacity2vehicle.get(idx);
            double price = priceTree.query(unit, source, dest, vehicle);
            int totalCapacity = capacity.get(idx).stream().mapToInt(Integer::intValue).sum();
            objective.setCoefficient(result[j], weight1 * price * totalCapacity / 1e8 - weight2 * totalCapacity);
        }
        objective.setMinimization();

        // Giải bài toán tối ưu
        MPSolver.ResultStatus status = solver.solve();

        if (status == MPSolver.ResultStatus.OPTIMAL || status == MPSolver.ResultStatus.FEASIBLE) {
            int[] solution = new int[n];
            for (int j = 0; j < n; j++) {
                solution[j] = (int) result[j].solutionValue();
                if (solution[j] > 0) {
                    int idx = filteredCombinations.get(j);
                    String unit = capacity2unit.get(idx);
                    deliveryQuantity.get(source).get(unit).put(date,
                            deliveryQuantity.get(source).get(unit).get(date) - solution[j]);
                }
            }
            return solution;
        } else {
            System.out.println("No optimal solution found.");
            return new int[n];
        }
    }
}
