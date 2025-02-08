package org.toyotatransportationplanning;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.text.*;

@RestController
@RequestMapping("/api")
public class Main {

    @PostMapping("/process")
    public ResponseEntity<ResultResponse> mainProcess(@RequestParam("excelFile") MultipartFile excelFile,
                                                      @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                                                      @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        if (excelFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Order> allOrders;
        Map<String, Map<String, Map<Date, Integer>>> deliveryQuantity;
        Map<String, List<List<Integer>>> capacityMatrix;
        PriceTree priceTree;
        File tempFile;
        try {
            tempFile = File.createTempFile("temp", ".xlsx");
            tempFile.deleteOnExit();
            excelFile.transferTo(tempFile);
            String filePath = tempFile.getAbsolutePath();
            allOrders = ReadData.readOrders(filePath);
            priceTree = ReadData.readPrice(filePath);
            deliveryQuantity = ReadData.readQuantity(filePath);
            capacityMatrix = ReadData.readCapacity(filePath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Nhập ngày
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        // In All Orders
        System.out.println("===== Danh sách đơn hàng =====");
        for (Order order : allOrders) {
            System.out.println("  ID: " + order.getId());
            System.out.println("  Grade: " + order.getGrade());
            System.out.println("  Source: " + order.getSource());
            System.out.println("  Destination: " + order.getDestination());
            System.out.println("  Minimum Start Date: " + order.getDate());
            System.out.println("----------------------");
        }

        // In Delivery Quantity
        System.out.println("===== Giới hạn giao hàng hằng ngày =====");
        for (Map.Entry<String, Map<String, Map<Date, Integer>>> sourceEntry : deliveryQuantity.entrySet()) {
            String source = sourceEntry.getKey();
            System.out.println("Source: " + source);

            for (Map.Entry<String, Map<Date, Integer>> unitEntry : sourceEntry.getValue().entrySet()) {
                String unit = unitEntry.getKey();
                System.out.println("  Unit: " + unit);

                for (Map.Entry<Date, Integer> dateEntry : unitEntry.getValue().entrySet()) {
                    Date date = dateEntry.getKey();
                    int quantity = dateEntry.getValue();
                    System.out.println("    Date: " + date + ", Quantity: " + quantity);
                }
            }
        }

        // Xử lý order
        List<Order> orders = new ArrayList<>();
        for (Order order : allOrders) {
            if (!order.getDate().before(startDate) && !order.getDate().after(endDate)) {
                orders.add(order);
            }
        }

        // Xử lý TransportUnit
        Map<String, Integer> mapTransportUnit = new HashMap<>();
        mapTransportUnit.put("GRL", 0);
        mapTransportUnit.put("PA", 1);
        mapTransportUnit.put("PSC", 2);
        mapTransportUnit.put("VTC", 3);
        List<String> transportUnits = new ArrayList<>(mapTransportUnit.keySet());

        // Xử lý Destination
        List<String> destinations = orders.stream()
                .map(Order::getDestination)
                .distinct()
                .toList();

//        // Xử lý Vehicle
//        Map<String, Integer> mapVehicle = new HashMap<>();
//        int index = 0;
//        for (String vehicle : capacityMatrix.keySet()) {
//            mapVehicle.put(vehicle, index++);
//        }
//        List<String> vehicles = new ArrayList<>(mapVehicle.keySet());

//        // Xử lý Date
//        List<Date> minStartDate = orders.stream()
//                .map(Order::getDate)
//                .sorted()
//                .toList();

        // Xử lý Source
        List<String> sources = List.of("TMV", "KGL", "South Yard");

        // Xử lý capacity, capacity2vehicle, capacity2unit
        List<List<Integer>> capacity = new ArrayList<>();
        Map<Integer, String> capacity2vehicle = new HashMap<>();
        Map<Integer, String> capacity2unit = new HashMap<>();
        int startLoop = 0;
        for (String unit : transportUnits) {
            for (String item : capacityMatrix.keySet()) {
                List<List<Integer>> matrixData = capacityMatrix.get(item);
                capacity.addAll(matrixData);
                for (int i = startLoop; i < capacity.size(); i++) {
                    capacity2vehicle.put(i, item);
                    capacity2unit.put(i, unit);
                }
                startLoop = capacity.size();
            }
        }

        // Định nghĩa các giới hạn
        int countCars = orders.size();
        int countCombinations = capacity.size();
        int countTransportUnits = mapTransportUnit.size();
        int countDestinations = destinations.size();
        double totalPrice = 0;
        int carCount = 0;
        int vehicleID = 0;

        for (Date date = startDate; !date.after(endDate); date = Date.from(date.toInstant().plus(1, ChronoUnit.DAYS))) {
            System.out.println("Xét ngày " + dateFormat.format(date));
            for (String source : sources) {
                for (String dest : destinations) {
                    // Lọc dữ liệu theo bộ 3 (Date, Source, Dest)
                    List<Order> filteredOrders = new ArrayList<>();
                    for (Order order : orders) {
                        if (dateFormat.format(order.getDate()).equals(dateFormat.format(date))
                                && order.getSource().equals(source)
                                && order.getDestination().equals(dest)) {
                            filteredOrders.add(order);
                        }
                    }
                    // Lọc các tổ hợp
                    List<Integer> filteredCombinations = new ArrayList<>();
                    for (int j = 0; j < countCombinations; j++) {
                        if (priceTree.query(capacity2unit.get(j), source, dest, capacity2vehicle.get(j)) != PriceTree.PRICE_INF) {
                            filteredCombinations.add(j);
                        }
                    }
                    // Tính số xe mỗi loại
                    int[] quantityEachSize = new int[7];
                    for (Order order : filteredOrders) {
                        int sizeIndex = order.getSize();
                        if (sizeIndex >= 0 && sizeIndex < 7) {
                            quantityEachSize[sizeIndex]++;
                        }
                    }
                    int[] result = SolverUtil.scip(quantityEachSize, filteredCombinations, source, dest, date, capacity,
                            capacity2unit, capacity2vehicle, deliveryQuantity, priceTree);
                    // Gán xe cho dữ liệu đã lọc
                    boolean[] alreadyAssigned = new boolean[filteredOrders.size()];
                    for (int j = 0; j < result.length; j++) {
                        while (result[j] > 0) {
                            List<Integer> currentCombination = new ArrayList<>(capacity.get(filteredCombinations.get(j)));
                            totalPrice += priceTree.query(capacity2unit.get(filteredCombinations.get(j)), source, dest,
                                    capacity2vehicle.get(filteredCombinations.get(j))) * currentCombination.stream().mapToInt(Integer::intValue).sum();

                            for (int i = 0; i < filteredOrders.size(); i++) {
                                Order order = filteredOrders.get(i);
                                if (currentCombination.get(filteredOrders.get(i).getSize()) > 0 && !alreadyAssigned[i]) {
                                    currentCombination.set(filteredOrders.get(i).getSize(), currentCombination.get(filteredOrders.get(i).getSize()) - 1);
                                    alreadyAssigned[i] = true;
                                    order.setAssignedVehicleID(vehicleID);
                                    order.setAssignedVehicle(capacity2vehicle.get(filteredCombinations.get(j)));
                                    order.setAssignedUnit(capacity2unit.get(filteredCombinations.get(j)));
                                    carCount++;
                                }
                                if (currentCombination.stream().allMatch(v -> v == 0)) {
                                    break;
                                }
                            }
                            result[j]--;
                            vehicleID++;
                        }
                    }
                }
            }
        }
//        String exportPath = "Result/result.csv"; // Đường dẫn file xuất ra
//        try (CSVWriter writer = new CSVWriter(new FileWriter(exportPath))) {
//            String[] header = {"Grade", "ID", "Date", "Source", "Destination", "Assigned Vehicle ID", "Assigned Vehicle", "Assigned Unit"};
//            writer.writeNext(header);
//            for (Order order : orders) {
//                String[] row = {
//                        order.getGrade(),
//                        String.valueOf(order.getId()),
//                        order.getDate().toString(),
//                        order.getSource(),
//                        order.getDestination(),
//                        String.valueOf(order.getAssignedVehicleID()),
//                        order.getAssignedVehicle(),
//                        order.getAssignedUnit()
//                };
//                writer.writeNext(row);
//            }
//            System.out.println("Xuất file CSV thành công: " + exportPath);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Hoàn tất! Nhấn Enter để thoát...");
//        try {
//            System.in.read(); // Chờ người dùng nhấn Enter mới đóng console
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        ResultResponse resultResponse = new ResultResponse(orders, totalPrice, carCount);
        return ResponseEntity.ok(resultResponse);
    }
}