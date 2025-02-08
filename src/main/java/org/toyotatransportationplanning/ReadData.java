package org.toyotatransportationplanning;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ReadData {

    public static Map<String, Integer> mapSize = new HashMap<>() {{
        put("CV5", 0);
        put("CV4", 1);
        put("CV3", 2);
        put("CV2", 3);
        put("CV-1.5", 4);
        put("CV1", 5);
        put("PC", 6);
    }};

    public static List<Order> readOrders(String filePath) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath));
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheetPattern = workbook.getSheet("standard pattern");
        Sheet sheetRaw = workbook.getSheet("Raw data");

        Map<String, Integer> modelSizeDict = new HashMap<>();
        for (int i = 1; i < 100; i++) {
            Row row = sheetPattern.getRow(i);
            String grade = row.getCell(0).getStringCellValue();
            String size = row.getCell(1).getStringCellValue();
            modelSizeDict.put(grade, mapSize.getOrDefault(size, -1));
        }

        List<Order> orders = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < sheetRaw.getPhysicalNumberOfRows()-1; rowIndex++) {
            Row row = sheetRaw.getRow(rowIndex);
            String grade = row.getCell(0).getStringCellValue();
            String id = row.getCell(1).getStringCellValue();
            Date date = row.getCell(2).getDateCellValue();
            String source = row.getCell(3).getStringCellValue();
            String destination = row.getCell(4).getStringCellValue();

            Integer size = modelSizeDict.get(grade);
            if (size != null) {
                orders.add(new Order(grade, id, date, source, destination, size));
            }
        }

        workbook.close();
        return orders;
    }

    public static PriceTree readPrice(String filePath) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheetPrice = workbook.getSheet("Giá vận chuyển");

        PriceTree priceTree = new PriceTree();

        for (int rowIndex = 1; rowIndex <= sheetPrice.getPhysicalNumberOfRows()-1; rowIndex++) {
            String transportUnit = sheetPrice.getRow(rowIndex).getCell(0).getStringCellValue();
            String source = sheetPrice.getRow(rowIndex).getCell(1).getStringCellValue();
            String destination = sheetPrice.getRow(rowIndex).getCell(2).getStringCellValue();
            String vehicle = sheetPrice.getRow(rowIndex).getCell(3).getStringCellValue();
            double price = sheetPrice.getRow(rowIndex).getCell(4).getNumericCellValue();

            priceTree.insert(transportUnit, source, destination, vehicle, price);
        }

        workbook.close();
        return priceTree;
    }

    public static Map<String, Map<String, Map<Date, Integer>>> readQuantity(String filePath) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath));
        Workbook wb = new XSSFWorkbook(file);

        String[] sources = {"TMV", "KGL", "South Yard"};
        Map<String, Map<String, Map<Date, Integer>>> deliveryQuantity = new HashMap<>();

        for (String source : sources) {
            Sheet sheet = wb.getSheet("Delivery quantity " + source);
            if (sheet == null) continue;
            Row firstRow = sheet.getRow(0);
            if (firstRow == null) continue;
            List<Date> dateList = new ArrayList<>();
            for (int col = 1; col < firstRow.getLastCellNum(); col++) {
                Cell cell = firstRow.getCell(col);
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    dateList.add(cell.getDateCellValue());
                }
            }
            Map<String, Map<Date, Integer>> sourceData = new HashMap<>();
            for (int rowIndex = 2; rowIndex <= 5; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Cell unitCell = row.getCell(0);
                if (unitCell == null || unitCell.getCellType() != CellType.STRING) continue;
                String transportUnit = unitCell.getStringCellValue().trim();
                if (transportUnit.equalsIgnoreCase("total")) continue;

                Map<Date, Integer> unitData = new HashMap<>();
                for (int col = 1; col <= dateList.size(); col++) {
                    Cell cell = row.getCell(col);
                    int quantity = (cell != null && cell.getCellType() == CellType.NUMERIC) ? (int) cell.getNumericCellValue() : 0;
                    unitData.put(dateList.get(col - 1), quantity);
                }
                sourceData.put(transportUnit, unitData);
            }
            deliveryQuantity.put(source, sourceData);
        }

        wb.close();
        return deliveryQuantity;
    }

    public static Map<String, List<List<Integer>>> readCapacity(String filePath) throws IOException {
        FileInputStream file = new FileInputStream(new File(filePath));
        Workbook wb = new XSSFWorkbook(file);
        Sheet sheet = wb.getSheet("standard pattern");

        Map<String, List<List<Integer>>> capacityMatrix = new HashMap<>();
        capacityMatrix.put("CC 6", readCombination(sheet, "G", "M"));
        capacityMatrix.put("CC 5", readCombination(sheet, "P", "V"));
        capacityMatrix.put("CC 4", readCombination(sheet, "Y", "AE"));
        capacityMatrix.put("Truck 4", readCombination(sheet, "AH", "AM"));
        capacityMatrix.put("Truck 2", readCombination(sheet, "AP", "AU"));
        capacityMatrix.put("Short Truck", readCombination(sheet, "CD", "CJ"));
        capacityMatrix.put("VesselC61wCont3", readCombination(sheet, "BE", "BJ"));
        capacityMatrix.put("VesselC51wCont3", readCombination(sheet, "BM", "BQ"));

        capacityMatrix.put("VesselT41wCont3", capacityMatrix.get("Truck 4"));
        capacityMatrix.put("VesselT42wCont3", capacityMatrix.get("Truck 4"));
        capacityMatrix.put("Truck 1", capacityMatrix.get("Short Truck"));
        capacityMatrix.put("Self-driving", capacityMatrix.get("Short Truck"));
        capacityMatrix.put("VesselC62wCont3", capacityMatrix.get("VesselC61wCont3"));
        capacityMatrix.put("VesselC52wCont3", capacityMatrix.get("VesselC51wCont3"));

        wb.close();
        return capacityMatrix;
    }

    private static List<List<Integer>> readCombination(Sheet sheet, String startCol, String endCol) {
        List<List<Integer>> data = new ArrayList<>();
        int startIdx = columnToIndex(startCol);
        int endIdx = columnToIndex(endCol);
        int rowIndex = 1;

        while (true) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) break;

            List<Integer> rowData = new ArrayList<>();
            boolean isEmpty = true;

            for (int col = startIdx; col <= endIdx; col++) {
                Cell cell = row.getCell(col);
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    int value = (int) cell.getNumericCellValue();
                    rowData.add(value);
                    isEmpty = false;
                } else {
                    rowData.add(0); // Thêm 0 nếu ô trống
                }
            }

            if (isEmpty) break;

            while (rowData.size() < 7) {
                rowData.add(0, 0);
            }

            data.add(rowData);
            rowIndex++;
        }
        return data;
    }

    private static int columnToIndex(String column) {
        int index = 0;
        for (char c : column.toCharArray()) {
            index = index * 26 + (c - 'A' + 1);
        }
        return index - 1;
    }
}
